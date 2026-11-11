package com.ridehailing.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private int rideId;
    private String razorPayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignatureId;
}
