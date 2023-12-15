package com.danamon.autochain.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OtpRequest {
    private String secret;
    private String counter;
    private int digits;
    private String code;
    private String email;
}