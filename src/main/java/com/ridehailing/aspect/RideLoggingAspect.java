package com.ridehailing.aspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class RideLoggingAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(RideLoggingAspect.class);

    @Around("execution(* com.ridehailing.service.RideEngine.*(..))")
    public Object trackExecutionTime(ProceedingJoinPoint jp) throws Throwable {

        long startTime = System.currentTimeMillis();

        Object result = jp.proceed();

        long endTime = System.currentTimeMillis();

        long timeTaken = endTime - startTime;

        LOGGER.info("AOP Log: [RideEngine] {}() method executed in {} ms ", jp.getSignature().getName(), timeTaken);

        return result;
    }

    @Before("execution(* com.ridehailing.controller.*.*(..))")
    public void logApiRequests(org.aspectj.lang.JoinPoint jp){
        LOGGER.info("AOP Log: API Request Received - Method: {}()", jp.getSignature().getName());
    }
}
