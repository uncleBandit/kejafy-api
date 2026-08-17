package com.pitahmzalendo.kejafy.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * app.mpesa.consumer-key=${MPESA_CONSUMER_KEY}
 * app.mpesa.consumer-secret=${MPESA_CONSUMER_SECRET}
 * app.mpesa.shortcode=${MPESA_SHORTCODE}
 * app.mpesa.passkey=${MPESA_PASSKEY}
 * app.mpesa.base-url=${MPESA_BASE_URL:https://sandbox.safaricom.co.ke}
 * app.mpesa.callback-url=${MPESA_CALLBACK_URL}
 *
 * base-url switches between sandbox.safaricom.co.ke and
 * api.safaricom.co.ke — keep it as one property rather than an
 * environment flag so staging can point at production Daraja for a
 * final smoke test without a code change.
 *
 * callback-url MUST be a publicly reachable HTTPS URL (Safaricom will
 * not call localhost) — e.g. https://api.kejafy.com/api/v1/payments/mpesa/callback.
 * In local dev, tunnel it (ngrok/cloudflared) and point this at the
 * tunnel URL.
 */
@Component
@ConfigurationProperties(prefix = "app.mpesa")
public class MpesaProperties {

    private String consumerKey;
    private String consumerSecret;
    private String shortcode;
    private String passkey;
    private String baseUrl;
    private String callbackUrl;

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public String getShortcode() {
        return shortcode;
    }

    public void setShortcode(String shortcode) {
        this.shortcode = shortcode;
    }

    public String getPasskey() {
        return passkey;
    }

    public void setPasskey(String passkey) {
        this.passkey = passkey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }
}
