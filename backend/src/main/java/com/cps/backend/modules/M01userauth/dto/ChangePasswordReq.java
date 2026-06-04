package com.cps.backend.modules.M01userauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordReq(
    @NotBlank String oldPassword,
    @NotBlank @Size(min = 6, max = 32) String newPassword
) {}
