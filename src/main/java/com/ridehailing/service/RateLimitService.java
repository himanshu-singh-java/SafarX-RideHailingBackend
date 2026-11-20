package com.ridehailing.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final long MAX_STALE_WINDOW_MS = 600_000L;

    private final ConcurrentHashMap<String, Deque<Long>> requestLogs = new ConcurrentHashMap<>();

    public boolean isAllowed(String key, int maxRequests, long windowMillis){
        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;
        boolean[] allowed = {false};

        requestLogs.compute(key, (k, timestamps) -> {
            if(timestamps == null){
                timestamps = new ArrayDeque<>();
            }
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart){
                timestamps.pollFirst();
            }
            if(timestamps.size() < maxRequests){
                timestamps.addLast(now);
                allowed[0] = true;
            }
            return timestamps;
        });
        return allowed[0];
    }

    @Scheduled(fixedRate = 600_000)
    public void cleanupStaleBuckets(){
        long cutoff = System.currentTimeMillis() - MAX_STALE_WINDOW_MS;
        requestLogs.entrySet().removeIf( entry -> {
            Deque<Long> deque = entry.getValue();
            return deque.isEmpty() || deque.peekLast() < cutoff;
        });
    }
}
