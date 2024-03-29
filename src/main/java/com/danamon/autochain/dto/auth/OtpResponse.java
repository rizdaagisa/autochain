package com.danamon.autochain.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URL;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OtpResponse {
    private String secret;
    private String code;
    private String period;
    private String email;
    private String issuer;
    private URL url;
}
