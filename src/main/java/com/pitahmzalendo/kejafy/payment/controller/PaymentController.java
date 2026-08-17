package com.pitahmzalendo.kejafy.payment.controller;

import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import com.pitahmzalendo.kejafy.payment.dto.request.InitiateCardPaymentRequest;
import com.pitahmzalendo.kejafy.payment.dto.request.InitiateMpesaPaymentRequest;
import com.pitahmzalendo.kejafy.payment.dto.response.CardPaymentInitiationResponse;
import com.pitahmzalendo.kejafy.payment.dto.response.MpesaPaymentInitiationResponse;
import com.pitahmzalendo.kejafy.payment.dto.response.PaymentStatusResponse;
import com.pitahmzalendo.kejafy.payment.service.PaymentService;
import com.pitahmzalendo.kejafy.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "M-Pesa and card checkout for subscriptions")
@SecurityRequirement(name = "sessionCookie")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/mpesa/initiate")
    @Operation(
            summary = "Start an M-Pesa STK push for a subscription plan",
            description = "Triggers the payment prompt on the customer's phone. Poll " +
                    "GET /api/v1/payments/{transactionId}/status for the result — there is " +
                    "no synchronous confirmation."
    )
    @ApiResponse(responseCode = "200", description = "STK push sent")
    @ApiResponse(responseCode = "502", description = "M-Pesa could not be reached or declined the request")
    public ResponseEntity<MpesaPaymentInitiationResponse> initiateMpesaPayment(
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody InitiateMpesaPaymentRequest request
    ) {
        User viewer = principal.getDomainUser();
        return ResponseEntity.ok(paymentService.initiateMpesaPayment(viewer, request));
    }

    @PostMapping("/stripe/initiate")
    @Operation(
            summary = "Start a card payment for a subscription plan",
            description = "Returns a Stripe PaymentIntent client secret for the client SDK " +
                    "(Payment Sheet / Elements) to confirm directly with Stripe."
    )
    @ApiResponse(responseCode = "200", description = "PaymentIntent created")
    public ResponseEntity<CardPaymentInitiationResponse> initiateCardPayment(
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody InitiateCardPaymentRequest request
    ) {
        User viewer = principal.getDomainUser();
        return ResponseEntity.ok(paymentService.initiateCardPayment(viewer, request));
    }

    @GetMapping("/{transactionId}/status")
    @Operation(summary = "Poll the status of a payment attempt")
    @ApiResponse(responseCode = "200", description = "Current status")
    @ApiResponse(responseCode = "404", description = "No such transaction for the caller")
    public ResponseEntity<PaymentStatusResponse> getStatus(
            @AuthenticationPrincipal KejafyOidcUser principal,
            @PathVariable Long transactionId
    ) {
        User viewer = principal.getDomainUser();
        return ResponseEntity.ok(PaymentStatusResponse.from(paymentService.getTransaction(transactionId, viewer)));
    }
}
