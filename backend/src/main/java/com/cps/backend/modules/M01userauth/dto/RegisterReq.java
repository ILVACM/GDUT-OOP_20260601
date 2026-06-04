package com.cps.backend.modules.M01userauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.cps.backend.modules.M01userauth.enums.UserType;

public record RegisterReq(
    @NotBlank @Size(min = 4, max = 30) String name,
    @NotBlank @Size(min = 6, max = 32) String password,
    @NotNull UserType type
) {}
