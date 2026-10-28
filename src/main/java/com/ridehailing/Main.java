package com.ridehailing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAspectJAutoProxy
@SpringBootApplication
public class Main {
    public static void main(String[] args) {

        System.out.println("Initializing Database and Systems...");

        ApplicationContext context = SpringApplication.run(Main.class);
    }
}