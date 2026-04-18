package com.automation.api.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponse {

    @JsonAlias({"access_token", "accessToken", "token"})
    private String accessToken;

    @JsonAlias({"refresh_token", "refreshToken"})
    private String refreshToken;

    @JsonAlias({"expires_in", "expiresIn"})
    private Long expiresInSeconds;

    @JsonAlias({"token_type", "tokenType"})
    private String tokenType;

    private String scope;
}
