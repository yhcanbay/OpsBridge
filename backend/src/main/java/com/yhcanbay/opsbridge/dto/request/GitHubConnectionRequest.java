package com.yhcanbay.opsbridge.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GitHubConnectionRequest {

    @NotBlank
    @Size(max = 80)
    private String name;

    // Token doğrulaması serviste yapılır; FieldError'ın rejectedValue alanına yazılmaz.
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String token;
}
