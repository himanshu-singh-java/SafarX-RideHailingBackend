package com.ridehailing.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.ridehailing.exception.ForbiddenException;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.exception.RideConflictException;
import com.ridehailing.model.PaymentStatus;
import com.ridehailing.model.RideStatus;
import com.ridehailing.model.Rides;
import com.ridehailing.repository.RideRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Slf4j
@Service
public class PaymentService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private final RideRepository rideRepository;

    public PaymentService(RideRepository rideRepository){
        this.rideRepository = rideRepository;
    }

    @Transactional
    public String createOrder(int rideId, String riderEmail) throws RazorpayException {

        Rides ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Ride not found!"));

        if(!ride.getRider().getUsers().getEmail().equals(riderEmail)){
            throw new ForbiddenException("Error: Unauthorized! You can only pay for your own rides.");
        }

        if(ride.getRideStatus() != RideStatus.COMPLETED){
            throw new RideConflictException("Error: Payment can only be initiated for COMPLETED rides. Current status: " + ride.getRideStatus());
        }

        if(ride.getPaymentStatus() == PaymentStatus.SUCCESS){
            throw new RideConflictException("Error: This ride is already paid.");
        }

        Double actualAmount = ride.getFare();

        if(actualAmount == null || actualAmount <= 0){
            throw new IllegalArgumentException("Error: Invalid fare amount for this ride.");
        }

        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        if(ride.getRazorPayOrderId() != null && !ride.getRazorPayOrderId().isBlank()){
            Order existingOrder = client.orders.fetch(ride.getRazorPayOrderId());
            return  existingOrder.toString();
        }

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", (int) Math.round(actualAmount * 100));
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "ride_receipt_" + rideId);

        Order order = client.orders.create(orderRequest);

        String generatedOrderId = order.get("id");
        ride.setRazorPayOrderId(generatedOrderId);
        rideRepository.save(ride);

        return order.toString();
    }

    @Transactional
    public boolean verifySignature(int rideId, String paymentId, String signature, String riderEmail){
        try{

            Rides ride = rideRepository.findById(rideId).
                    orElseThrow(() -> new ResourceNotFoundException("Error: Ride not found!"));

            if(!ride.getRider().getUsers().getEmail().equals(riderEmail)){
                throw new ForbiddenException("Error: Unauthorized! You can only verify payment for your own rides.");
            }

            if(ride.getPaymentStatus() == PaymentStatus.SUCCESS){
                return true;
            }

            String dbOrderId = ride.getRazorPayOrderId();

            if(dbOrderId == null || dbOrderId.isEmpty()){
                throw new RideConflictException("Error: No payment order was generated for this ride.");
            }

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", dbOrderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isAuthentic = Utils.verifyPaymentSignature(options, keySecret);

            if(isAuthentic){
                ride.setPaymentStatus(PaymentStatus.SUCCESS);
                rideRepository.save(ride);
            }
            return isAuthentic;
        }
        catch (RazorpayException e){
            log.error("Razorpay Signature Verification Failed: {}", e.getMessage());
            return false;
        }
    }
}
