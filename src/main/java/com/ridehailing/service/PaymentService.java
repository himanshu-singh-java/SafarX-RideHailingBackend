package com.ridehailing.service;

import com.razorpay.*;
import com.ridehailing.exception.*;
import com.ridehailing.model.PaymentStatus;
import com.ridehailing.model.RideStatus;
import com.ridehailing.model.Rides;
import com.ridehailing.repository.RideRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class PaymentService {

    private static final String EXPECTED_CURRENCY = "INR";

    private static final String ORDER_STATUS_PAID = "paid";
    private static final String ORDER_STATUS_CREATED = "created";
    private static final String ORDER_STATUS_ATTEMPTED = "attempted";

    private static final String PAYMENT_STATUS_CAPTURED = "captured";
    private static final String PAYMENT_STATUS_AUTHORIZED = "authorized";
    private static final String PAYMENT_STATUS_CREATED = "created";

    private static final String EVENT_PAYMENT_CAPTURED = "payment.captured";
    private static final String EVENT_PAYMENT_FAILED = "payment.failed";

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    private final RideRepository rideRepository;

    public PaymentService(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    @Transactional(noRollbackFor = RideConflictException.class)
    public String createOrder(int rideId, String riderEmail)  {

        Rides ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Ride not found!"));

        if (!ride.getRider().getUsers().getEmail().equalsIgnoreCase(riderEmail)) {
            throw new ForbiddenException("Error: Unauthorized! You can only pay for your own rides.");
        }

        if (ride.getRideStatus() != RideStatus.COMPLETED) {
            throw new RideConflictException("Error: Payment can only be initiated for " +
                    "COMPLETED rides. Current status: " + ride.getRideStatus());
        }

        if (ride.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new RideConflictException("Error: This ride is already paid.");
        }

        Double actualAmount = ride.getFare();

        if (actualAmount == null || actualAmount <= 0) {
            throw new IllegalArgumentException("Error: Invalid fare amount for this ride.");
        }

        long expectedAmount = toPaise(actualAmount);
        RazorpayClient client = createRazorPayClient();

        if (ride.getRazorPayOrderId() != null && !ride.getRazorPayOrderId().isBlank()) {

            try {
                Order existingOrder = client.orders.fetch(ride.getRazorPayOrderId());
                String orderStatus = asString(existingOrder.get("status"));
                long orderAmount = asLong(existingOrder.get("amount"));
                long amountPaid = asLong(existingOrder.get("amount_paid"));
                String orderCurrency = asString(existingOrder.get("currency"));

                if (ORDER_STATUS_PAID.equalsIgnoreCase(orderStatus)) {

                    if (orderAmount == expectedAmount && amountPaid == expectedAmount
                            && EXPECTED_CURRENCY.equalsIgnoreCase(orderCurrency)) {
                        ride.setPaymentStatus(PaymentStatus.SUCCESS);
                        rideRepository.save(ride);
                        log.info("Order {} for ride {} was already paid on Razorpay. Synced DB to SUCCESS.",
                                ride.getRazorPayOrderId(), rideId);
                        throw new RideConflictException("Error: This ride is already paid.");
                    }

                    log.error("Paid order mismatch for ride {} (order {}): orderAmount={}, amountPaid={}," +
                                    " expected={}, currency={}", rideId, ride.getRazorPayOrderId(), orderAmount,
                            amountPaid, expectedAmount, orderCurrency);

                    throw new RideConflictException("Error: Existing Razorpay order is paid" +
                            " but amount does not match. Manual reconciliation required.");
                }

                boolean isReusableStatus = ORDER_STATUS_CREATED.equalsIgnoreCase(orderStatus) ||
                        ORDER_STATUS_ATTEMPTED.equalsIgnoreCase(orderStatus);

                if (ride.getPaymentStatus() != PaymentStatus.FAILED && isReusableStatus
                        && orderAmount == expectedAmount && EXPECTED_CURRENCY.equalsIgnoreCase(orderCurrency)) {
                    return existingOrder.toString();
                }
                log.info("Recreating Razorpay order for ride {} (prev order: {}, status: {}, ridePaymentStatus: {})",
                        rideId, ride.getRazorPayOrderId(), orderStatus, ride.getPaymentStatus());
            } catch (RazorpayException e) {
                log.warn("Could not fetch existing Razorpay order {} for ride {}. Creating fresh" +
                        " order. Reason: {}", ride.getRazorPayOrderId(), rideId, e.getMessage());
            }
        }

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", expectedAmount);
            orderRequest.put("currency", EXPECTED_CURRENCY);
            orderRequest.put("receipt", "ride_" + rideId + "_" + System.currentTimeMillis());

            Order order = client.orders.create(orderRequest);

            String generatedOrderId = asString(order.get("id"));
            ride.setRazorPayOrderId(generatedOrderId);
            ride.setPaymentStatus(PaymentStatus.PENDING);
            rideRepository.save(ride);

            return order.toString();
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for ride {}: {}", rideId, e.getMessage());
            throw new PaymentGatewayException("Payment gateway is temporarily unavailable. Please try again.");
        }
    }

    @Transactional
    public boolean verifySignature(int rideId, String paymentId, String signature, String riderEmail) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("Error: razorpay_payment_id is required and cannot be blank.");
        }
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("Error: razorpay_signature is required and cannot be blank.");
        }

        String cleanPaymentId = paymentId.trim();
        String cleanSignature = signature.trim();


        Rides ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Ride not found!"));

        if (!ride.getRider().getUsers().getEmail().equalsIgnoreCase(riderEmail)) {
            throw new ForbiddenException("Error: Unauthorized! You can only verify payment for your own rides.");
        }

        String dbOrderId = ride.getRazorPayOrderId();

        if (dbOrderId == null || dbOrderId.isBlank()) {
            throw new RideConflictException("Error: No payment order was generated for this ride.");
        }

        if (ride.getFare() == null || ride.getFare() <= 0) {
            throw new IllegalArgumentException("Error: Invalid fare amount for this ride.");
        }

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", dbOrderId);
        options.put("razorpay_payment_id", cleanPaymentId);
        options.put("razorpay_signature", cleanSignature);

        boolean isSignatureValid;

        try{
            isSignatureValid = Utils.verifyPaymentSignature(options, keySecret);
        }
        catch (RazorpayException e){
            log.warn("Payment signature verification error for ride {}: {}", rideId, e.getMessage());
            return false;
        }

        if(!isSignatureValid){
            log.warn("Payment signature mismatch for ride {} and payment {}", rideId, cleanPaymentId);
            return false;
        }

        if(ride.getPaymentStatus() == PaymentStatus.SUCCESS
        && ride.getRazorPayPaymentId() != null
        && !ride.getRazorPayPaymentId().isBlank()){
            return ride.getRazorPayPaymentId().equals(cleanPaymentId);
        }

        Payment payment;
        try {
            RazorpayClient client = createRazorPayClient();
            payment = client.payments.fetch(cleanPaymentId);
        }
        catch (RazorpayException e){
            log.error("Razorpay API unreachable while fetching payment {} for ride {}: {}",
                    cleanPaymentId, rideId, e.getMessage());

            throw new PaymentGatewayException(
                    "Unable to confirm payment with Razorpay right now. Please retry shortly.");
        }

        String status = asString(payment.get("status"));
        String paymentOrderId = asString(payment.get("order_id"));
        String currency = asString(payment.get("currency"));
        long paidAmountPaise = asLong(payment.get("amount"));
        long expectedAmountPaise = toPaise(ride.getFare());

        if (!dbOrderId.equals(paymentOrderId)) {
            log.warn("Order ID mismatch for payment {}: expected {}, got {}", cleanPaymentId, dbOrderId, paymentOrderId);
            return false;
        }

        if (!EXPECTED_CURRENCY.equalsIgnoreCase(currency)) {
            log.warn("Currency mismatch for payment {}: expected {}, got {}", cleanPaymentId, EXPECTED_CURRENCY, currency);
            return false;
        }

        if (paidAmountPaise != expectedAmountPaise) {
            log.warn("Amount mismatch for ride {}: expected {} paise, got {} paise",
                    rideId, expectedAmountPaise, paidAmountPaise);
            return false;
        }

        if (PAYMENT_STATUS_AUTHORIZED.equalsIgnoreCase(status) || PAYMENT_STATUS_CREATED.equalsIgnoreCase(status)) {
            log.info("Payment {} for ride {} is in pending state: {}", cleanPaymentId, rideId, status);
            throw new RideConflictException("Payment is still processing and not yet captured. Please retry shortly.");
        }

        if (!PAYMENT_STATUS_CAPTURED.equalsIgnoreCase(status)) {
            log.warn("Payment {} for ride {} is not captured. Actual status: {}", cleanPaymentId, rideId, status);
            return false;
        }

        ride.setPaymentStatus(PaymentStatus.SUCCESS);
        ride.setRazorPayPaymentId(cleanPaymentId);
        rideRepository.save(ride);
        log.info("Payment {} verified and captured for ride {}", cleanPaymentId, rideId);
        return true;
    }

    @Transactional
    public void handleWebhook(String rawPayload, String webhookSignature){
        if(webhookSecret == null || webhookSecret.isBlank()){
            log.warn("RAZORPAY_WEBHOOK_SECRET is not configured!");
            throw new IllegalStateException("Webhook secret is not configured on the server.");
        }

        if(rawPayload == null || rawPayload.isBlank() || webhookSignature == null || webhookSignature.isBlank()){
            throw new UnauthorizedException("Missing webhook payload or signature.");
        }

        try {
            boolean isValid = Utils.verifyWebhookSignature(rawPayload, webhookSignature.trim(), webhookSecret);
            if(!isValid){
                log.warn("Rejected Razorpay webhook: invalid signature");
                throw new UnauthorizedException("Invalid Razorpay webhook signature.");
            }
        }
        catch (RazorpayException e){
            log.warn("Rejected Razorpay webhook signature verification error: {}", e.getMessage());
            throw new UnauthorizedException("Invalid Razorpay webhook signature.");
        }

        JSONObject root;

        try {
            root = new JSONObject(rawPayload);
        }
        catch (JSONException e){
            throw new IllegalArgumentException("Malformed webhook JSON payload.");
        }

        String event = root.optString("event", "");
        JSONObject payloadObj = root.optJSONObject("payload");
        JSONObject paymentWrapper = (payloadObj != null) ? payloadObj.optJSONObject("payment") : null;
        JSONObject paymentEntity = (paymentWrapper != null) ? paymentWrapper.optJSONObject("entity") : null;

        if (paymentEntity == null) {
            log.info("Ignoring Razorpay webhook event without payment entity: {}", event);
            return;
        }

        String orderId = paymentEntity.optString("order_id", "").trim();
        String paymentId = paymentEntity.optString("id", "").trim();

        if (orderId.isBlank()) {
            log.warn("Razorpay webhook event {} (payment {}) has no order_id. Skipping.", event, paymentId);
            return;
        }

        Rides ride = rideRepository.findByRazorPayOrderIdForUpdate(orderId).orElse(null);
        if (ride == null) {
            log.warn("No ride found for Razorpay order_id {} in webhook event {}. Skipping.", orderId, event);
            return;
        }

        if (EVENT_PAYMENT_CAPTURED.equals(event)) {
            String status = paymentEntity.optString("status", "");
            String currency = paymentEntity.optString("currency", "");
            long paidAmountPaise = paymentEntity.optLong("amount", -1L);
            long expectedAmountPaise = (ride.getFare() != null && ride.getFare() > 0) ? toPaise(ride.getFare()) : -1L;

            if (!PAYMENT_STATUS_CAPTURED.equalsIgnoreCase(status)
                    || !EXPECTED_CURRENCY.equalsIgnoreCase(currency)
                    || expectedAmountPaise <= 0
                    || paidAmountPaise != expectedAmountPaise) {
                log.error("Webhook validation failed for ride {} (order {}): status={}, currency={}, paid={}, expected={}",
                        ride.getRideId(), orderId, status, currency, paidAmountPaise, expectedAmountPaise);
                return;
            }

            if (ride.getPaymentStatus() == PaymentStatus.SUCCESS) {
                if ((ride.getRazorPayPaymentId() == null || ride.getRazorPayPaymentId().isBlank())
                        && !paymentId.isBlank()) {
                    ride.setRazorPayPaymentId(paymentId);
                    rideRepository.save(ride);
                }
                log.info("Webhook payment.captured already processed for ride {} (order {}).", ride.getRideId(), orderId);
                return;
            }

            ride.setPaymentStatus(PaymentStatus.SUCCESS);
            if (!paymentId.isBlank()) {
                ride.setRazorPayPaymentId(paymentId);
            }
            rideRepository.save(ride);
            log.info("Webhook synced ride {} to PaymentStatus.SUCCESS (payment {}, order {})",
                    ride.getRideId(), paymentId, orderId);

        } else if (EVENT_PAYMENT_FAILED.equals(event)) {
            if (ride.getPaymentStatus() != PaymentStatus.SUCCESS) {
                ride.setPaymentStatus(PaymentStatus.FAILED);
                rideRepository.save(ride);
                log.info("Webhook marked ride {} as PaymentStatus.FAILED (payment {}, order {})",
                        ride.getRideId(), paymentId, orderId);
            }
        } else {
            log.info("Unhandled Razorpay webhook event ignored: {}", event);
        }
    }

    private RazorpayClient createRazorPayClient(){
        try {
            return new RazorpayClient(keyId, keySecret);
        }
        catch (RazorpayException e){
            log.error("Failed to initialize RazorpayClient: {}", e.getMessage());
            throw new PaymentGatewayException("Payment gateway is temporarily unavailable. Please try again.");
        }
    }

    private long toPaise(Double fare){
        return BigDecimal.valueOf(fare)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private String asString(Object value){
        if(value == null || value == JSONObject.NULL){
            return "";
        }
        return String.valueOf(value);
    }

    private long asLong(Object value){
        if(value instanceof Number number){
            return number.longValue();
        }
        if(value instanceof String str && !str.isBlank()){
            try {
                return Long.parseLong(str.trim());
            }
            catch (NumberFormatException ignored){
                return  -1L;
            }
        }
        return -1L;
    }
}

