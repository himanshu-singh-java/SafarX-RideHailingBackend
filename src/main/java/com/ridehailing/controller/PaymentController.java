package com.ridehailing.controller;

import com.razorpay.RazorpayException;
import com.ridehailing.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping(value = "/create-order", produces = "application/json")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> data) throws RazorpayException {

        double amount = Double.parseDouble(data.get("amount").toString());

        String receipt = "txn_" + System.currentTimeMillis();

        String orderResponse = paymentService.createOrder(amount, receipt);

        return ResponseEntity.ok(orderResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> data){

        String orderId = data.get("razorpay_order_id");
        String paymentId = data.get("razorpay_payment_id");
        String signature = data.get("razorpay_signature");

        boolean isValid = paymentService.verifySignature(orderId, paymentId, signature);

        if(!isValid){
            throw new RuntimeException("Error: Invalid payment signature! Payment verification failed.");
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Payment verified successfully!"
        ));
    }
}
