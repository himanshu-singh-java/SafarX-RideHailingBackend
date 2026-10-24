package com.ridehailing.dto;

public class PaymentRequest {
    private int rideId;
    private String razorPayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignatureId;



    public int getRideId() {
        return rideId;
    }

    public void setRideId(int rideId) {
        this.rideId = rideId;
    }

    public String getRazorPayOrderId() {
        return razorPayOrderId;
    }

    public void setRazorPayOrderId(String razorPayOrderId) {
        this.razorPayOrderId = razorPayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpaySignatureId() {
        return razorpaySignatureId;
    }

    public void setRazorpaySignatureId(String razorpaySignatureId) {
        this.razorpaySignatureId = razorpaySignatureId;
    }
}
