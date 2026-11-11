package com.ridehailing.controller;

import com.razorpay.RazorpayException;
import com.ridehailing.dto.PaymentRequest;
import com.ridehailing.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService){
        this.paymentService = paymentService;
    }

    @PostMapping(value = "/create-order", produces = "application/json")
    public ResponseEntity<?> createOrder(@RequestBody PaymentRequest request, Principal principal) throws RazorpayException {

        String orderResponse = paymentService.createOrder(request.getRideId(), principal.getName());

        return ResponseEntity.ok(orderResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentRequest request, Principal principal){

        boolean isAuthentic = paymentService.verifySignature(
                request.getRideId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignatureId(),
                principal.getName()
        );

        if(!isAuthentic){
            throw new IllegalArgumentException("Error: Invalid payment signature! Possible tampering detected.");
        }

        Map<String, String > response = new HashMap<>();
        response.put("message", "Payment verified and ride marked as PAID successfully!");

        return ResponseEntity.ok(response);
    }
}
