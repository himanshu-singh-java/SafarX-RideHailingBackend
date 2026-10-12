package com.ridehailing.exception;

public class RideConflictException extends RuntimeException{
    public RideConflictException(String message){
        super(message);
    }
}
