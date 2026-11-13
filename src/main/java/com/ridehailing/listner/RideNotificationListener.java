package com.ridehailing.listner;

import com.ridehailing.event.RideBroadcastEvent;
import com.ridehailing.event.RideNotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class RideNotificationListener {

    private final SimpMessagingTemplate messagingTemplate;

    public RideNotificationListener(SimpMessagingTemplate messagingTemplate){
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRideBroadcast(RideBroadcastEvent event){
        for(Integer driverId : event.getDriversId()){
            messagingTemplate.convertAndSend("/topic/driver/" + driverId, event.getRideResponse());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleGenericNotification(RideNotificationEvent event){
        messagingTemplate.convertAndSend(event.getTopic(), event.getPayload());
        log.info("Safe WebSocket Fire: Message sent to {} AFTER DB commit", event.getTopic());
    }
}
