package com.ridehailing.event;
import com.ridehailing.dto.RideResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class RideBroadcastEvent {
    private final int rideId;
    private final RideResponse rideResponse;
    private final List<Integer> driversId;
}
