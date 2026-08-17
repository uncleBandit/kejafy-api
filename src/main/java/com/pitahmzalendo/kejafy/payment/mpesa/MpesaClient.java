package com.pitahmzalendo.kejafy.payment.mpesa;

import com.pitahmzalendo.kejafy.payment.config.MpesaProperties;
import com.pitahmzalendo.kejafy.payment.dto.response.StkPushResponse;
import com.pitahmzalendo.kejafy.payment.dto.response.StkStatusQueryResponse;
import com.pitahmzalendo.kejafy.payment.exception.PaymentProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Thin adapter over Safaricom Daraja's STK Push (Lipa na M-Pesa Online)
 * API. There's no official Java SDK for Daraja, so this talks to it
 * over plain HTTP with RestClient — see PaymentService for the
 * orchestration that calls this (creating the PaymentTransaction row,
 * handling the callback).
 *
 * Three calls: getAccessToken (OAuth, cached in Redis — token is valid
 * ~1hr and shared across app instances, so caching it beats re-authing
 * on every STK push), stkPush (triggers the prompt on the customer's
 * phone), and queryStkStatus (reconciliation fallback if the callback
 * never arrives).
 */
@Component
public class MpesaClient {

    private static final Logger log = LoggerFactory.getLogger(MpesaClient.class);
    private static final String TOKEN_CACHE_KEY = "mpesa:access_token";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    // Accepts 07XXXXXXXX, 01XXXXXXXX, +2547XXXXXXXX, 2547XXXXXXXX, 2541XXXXXXXX
    private static final Pattern LOCAL_PREFIX = Pattern.compile("^0(7|1)");

    private final MpesaProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final RestClient restClient;

    public MpesaClient(MpesaProperties properties,
                       StringRedisTemplate redisTemplate,
                       RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    /**
     * Triggers the STK push prompt on the customer's phone. Returns the
     * CheckoutRequestID/MerchantRequestID pair PaymentService stores on
     * the PaymentTransaction so the eventual callback (or a status
     * query) can be matched back to it.
     */
    public StkPushResponse stkPush(String phoneNumber, BigDecimal amount, String accountReference,
                                   String transactionDesc) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String password = Base64.getEncoder().encodeToString(
                (properties.getShortcode() + properties.getPasskey() + timestamp).getBytes());

        Map<String, Object> body = Map.of(
                "BusinessShortCode", properties.getShortcode(),
                "Password", password,
                "Timestamp", timestamp,
                "TransactionType", "CustomerPayBillOnline",
                // Daraja wants a whole-number string amount — KES has no
                // meaningful sub-unit for STK push purposes.
                "Amount", amount.setScale(0, java.math.RoundingMode.CEILING).toBigInteger().toString(),
                "PartyA", normalizedPhone,
                "PartyB", properties.getShortcode(),
                "PhoneNumber", normalizedPhone,
                "CallBackURL", properties.getCallbackUrl(),
                "AccountReference", accountReference,
                "TransactionDesc", transactionDesc
        );

        try {
            StkPushResponse response = restClient.post()
                    .uri("/mpesa/stkpush/v1/processrequest")
                    .headers(h -> h.setBearerAuth(getAccessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(StkPushResponse.class);

            if (response == null || !response.accepted()) {
                String description = response != null ? response.responseDescription() : "empty response";
                throw new PaymentProviderException("M-Pesa declined the STK push request: " + description);
            }
            return response;
        } catch (RestClientException ex) {
            log.error("STK push call to Daraja failed", ex);
            throw new PaymentProviderException("Could not reach M-Pesa. Please try again shortly.", ex);
        }
    }

    /** Reconciliation fallback — see PaymentReconciliationService. */
    public StkStatusQueryResponse queryStkStatus(String checkoutRequestId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String password = Base64.getEncoder().encodeToString(
                (properties.getShortcode() + properties.getPasskey() + timestamp).getBytes());

        Map<String, Object> body = Map.of(
                "BusinessShortCode", properties.getShortcode(),
                "Password", password,
                "Timestamp", timestamp,
                "CheckoutRequestID", checkoutRequestId
        );

        try {
            return restClient.post()
                    .uri("/mpesa/stkpushquery/v1/query")
                    .headers(h -> h.setBearerAuth(getAccessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(StkStatusQueryResponse.class);
        } catch (RestClientException ex) {
            log.error("STK status query to Daraja failed for checkoutRequestId={}", checkoutRequestId, ex);
            throw new PaymentProviderException("Could not reach M-Pesa to check payment status.", ex);
        }
    }

    private String getAccessToken() {
        String cached = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        String credentials = Base64.getEncoder().encodeToString(
                (properties.getConsumerKey() + ":" + properties.getConsumerSecret()).getBytes());

        try {
            Map<String, Object> response = restClient.get()
                    .uri("/oauth/v1/generate?grant_type=client_credentials")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("access_token") == null) {
                throw new PaymentProviderException("M-Pesa did not return an access token.");
            }
            String token = response.get("access_token").toString();
            int expiresIn = response.get("expires_in") != null
                    ? Integer.parseInt(response.get("expires_in").toString())
                    : 3599;

            // Cache for a bit less than the real TTL so we never hand out
            // a token that expires mid-request.
            long cacheSeconds = Math.max(60, expiresIn - 60);
            redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, token, Duration.ofSeconds(cacheSeconds));
            return token;
        } catch (RestClientException ex) {
            log.error("Failed to obtain M-Pesa access token", ex);
            throw new PaymentProviderException("Could not authenticate with M-Pesa. Please try again shortly.", ex);
        }
    }

    private String normalizePhoneNumber(String rawPhoneNumber) {
        String digitsOnly = rawPhoneNumber.replaceAll("[^0-9]", "");
        if (digitsOnly.startsWith("254") && digitsOnly.length() == 12) {
            return digitsOnly;
        }
        if (LOCAL_PREFIX.matcher(digitsOnly).find() && digitsOnly.length() == 10) {
            return "254" + digitsOnly.substring(1);
        }
        throw new PaymentProviderException(
                "'" + rawPhoneNumber + "' doesn't look like a valid Safaricom number. " +
                        "Use a format like 07XXXXXXXX or 2547XXXXXXXX.");
    }
}
