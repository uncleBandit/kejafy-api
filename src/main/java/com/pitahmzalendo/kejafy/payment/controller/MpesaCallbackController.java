package com.pitahmzalendo.kejafy.payment.controller;

import com.pitahmzalendo.kejafy.payment.dto.response.MpesaCallbackPayload;
import com.pitahmzalendo.kejafy.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Server-to-server endpoint Safaricom posts to once an STK push
 * resolves — set as app.mpesa.callback-url. This is deliberately
 * permitAll in SecurityConfig (Safaricom can't do a session/OAuth
 * login) and NOT trusted on its own: PaymentService re-queries Daraja
 * directly before marking anything paid, so a spoofed POST to this URL
 * can't fake a payment.
 *
 * IN PRODUCTION: also restrict this path at the load balancer/firewall
 * to Safaricom's published callback IP ranges — defense in depth on
 * top of the re-query, not a replacement for it.
 *
 * Always returns 200 with ResultCode 0 regardless of what we did with
 * the payload — that's Daraja's ack contract. Returning anything else
 * makes Safaricom retry the callback repeatedly.
 */
@RestController
@Hidden
public class MpesaCallbackController {

    private static final Logger log = LoggerFactory.getLogger(MpesaCallbackController.class);

    private final PaymentService paymentService;

    public MpesaCallbackController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/v1/payments/mpesa/callback")
    public ResponseEntity<Map<String, Object>> handleCallback(@RequestBody MpesaCallbackPayload payload) {
        try {
            paymentService.handleMpesaCallback(payload);
        } catch (Exception ex) {
            // Never let a processing error turn into a non-200 here —
            // log it and still ack, or Safaricom will retry the same
            // callback on a schedule we don't control. The
            // reconciliation sweep is the backstop for anything this
            // swallows.
            log.error("Error processing M-Pesa callback", ex);
        }
        return ResponseEntity.ok(Map.of("ResultCode", 0, "ResultDesc", "Accepted"));
    }
}
