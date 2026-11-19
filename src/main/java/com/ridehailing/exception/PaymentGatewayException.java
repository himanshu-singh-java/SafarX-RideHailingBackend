package com.ridehailing.exception;

public class PaymentGatewayException extends RuntimeException{
    public PaymentGatewayException(String message){
        super(message);
    }
}
