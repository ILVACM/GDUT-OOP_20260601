package com.cps.backend.modules.M01userauth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginReq(
    @NotBlank String name,
    @NotBlank String password
) {}
