package com.ridehailing.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RideNotificationEvent {

    private final String topic;
    private final Object payload;
}
