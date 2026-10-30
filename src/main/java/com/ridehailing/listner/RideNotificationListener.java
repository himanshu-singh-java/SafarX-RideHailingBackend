package com.ridehailing.listner;

import com.ridehailing.event.RideBroadcastEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RideNotificationListener {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public RideNotificationListener(SimpMessagingTemplate messagingTemplate){
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRideBroadcast(RideBroadcastEvent event){
        for(Integer driverId : event.getDriversId()){
            messagingTemplate.convertAndSend("/topic/driver/" + driverId, event.getRideResponse());
        }
    }
}
