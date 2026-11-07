package com.ridehailing.event;

public class RideNotificationEvent {

    private final String topic;
    private final Object payload;

    public RideNotificationEvent(String topic, Object payload){
        this.topic = topic;
        this.payload = payload;
    }

    public String getTopic() {
        return topic;
    }

    public Object getPayload() {
        return payload;
    }
}
