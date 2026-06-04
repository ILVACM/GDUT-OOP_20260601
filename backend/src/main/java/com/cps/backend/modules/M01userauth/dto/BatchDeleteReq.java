package com.cps.backend.modules.M01userauth.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchDeleteReq(@NotEmpty List<Integer> ids) {}
