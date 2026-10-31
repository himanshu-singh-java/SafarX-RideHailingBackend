package com.ridehailing.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.ridehailing.exception.ForbiddenException;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.exception.RideConflictException;
import com.ridehailing.model.Rides;
import com.ridehailing.repository.RideRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Autowired
    private RideRepository rideRepository;

    public String createOrder(int rideId, String riderEmail) throws RazorpayException {

        Rides ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Ride not found!"));

        if(!ride.getRider().getUsers().getEmail().equals(riderEmail)){
            throw new ForbiddenException("\"Error: Unauthorized! You can only pay for your own rides.\"");
        }

        if(ride.getPaymentStatus().equals("PAID")){
            throw new RideConflictException("Error: This ride is already paid.");
        }

        double actualAmount = ride.getFare();

        RazorpayClient client = new RazorpayClient(keyId, keySecret);

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
    public boolean verifySignature(int rideId, String paymentId, String signature){
        try{

            Rides ride = rideRepository.findById(rideId).
                    orElseThrow(() -> new ResourceNotFoundException("Error: Ride not found!"));

            if(ride.getPaymentStatus().equals("PAID")){
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
                ride.setPaymentStatus("PAID");
                rideRepository.save(ride);
            }
            return isAuthentic;
        }
        catch (RazorpayException e){
            System.out.println("Razorpay Signature Verification Failed: " + e.getMessage());
            return false;
        }
    }
}
