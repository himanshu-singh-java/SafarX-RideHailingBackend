package com.ridehailing.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    public String createOrder(double amount, String receiptId) throws RazorpayException {

        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", (int) Math.round(amount * 100));
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receiptId);

        Order order = client.orders.create(orderRequest);

        return order.toString();
    }

    public boolean verifySignature(String orderId, String paymentId, String signature){
        try{
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            return Utils.verifyPaymentSignature(options, keySecret);
        }
        catch (RazorpayException e){
            return false;
        }
    }
}
