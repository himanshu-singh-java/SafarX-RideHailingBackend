package com.ridehailing.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.ridehailing.exception.ResourceNotFoundException;
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

    public String createOrder(int rideId) throws RazorpayException {

        Rides ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Ride not found!"));

        double actualAmount = ride.getFare();

        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", (int) Math.round(actualAmount * 100));
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "ride_receipt_" + rideId);

        Order order = client.orders.create(orderRequest);

        return order.toString();
    }

    @Transactional
    public boolean verifySignature(int rideId, String orderId, String paymentId, String signature){
        try{
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isAuthentic = Utils.verifyPaymentSignature(options, keySecret);

            if(isAuthentic){
                Rides ride = rideRepository.findById(rideId)
                        .orElseThrow(() -> new ResourceNotFoundException("Error: Ride not found!"));
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
