package com.cps.backend.modules.M01userauth.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusReq(
    @NotNull Integer status
) {}
