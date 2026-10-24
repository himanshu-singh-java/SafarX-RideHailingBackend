package com.ridehailing.controller;

import com.razorpay.RazorpayException;
import com.ridehailing.dto.PaymentRequest;
import com.ridehailing.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping(value = "/create-order", produces = "application/json")
    public ResponseEntity<?> createOrder(@RequestBody PaymentRequest request) throws RazorpayException {

        String orderResponse = paymentService.createOrder(request.getRideId());

        return ResponseEntity.ok(orderResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentRequest request){

        boolean isAuthentic = paymentService.verifySignature(
                request.getRideId(),
                request.getRazorPayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignatureId()
        );

        if(!isAuthentic){
            throw  new RuntimeException("Error: Invalid payment signature! Possible tampering detected.");
        }

        Map<String, String > response = new HashMap<>();
        response.put("message", "Payment verified and ride marked as PAID successfully!");

        return ResponseEntity.ok(response);
    }
}
