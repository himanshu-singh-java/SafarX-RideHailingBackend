package com.ridehailing.dto;

import lombok.Data;

@Data
public class ProfileCompletionRequest {
    private String name;
    private String phone;
    private String role;
}
