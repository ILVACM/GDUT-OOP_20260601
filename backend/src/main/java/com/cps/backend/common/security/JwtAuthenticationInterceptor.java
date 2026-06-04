package com.cps.backend.common.security;

import com.cps.backend.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (requireRole != null) {
                throw new BusinessException(4101, "未登录");
            }
            return true;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(4102, "Token 已过期");
        }

        Claims claims = jwtUtil.parseToken(token);
        request.setAttribute("userId", Integer.valueOf(claims.getSubject()));
        request.setAttribute("userName", claims.get("name", String.class));
        request.setAttribute("userType", claims.get("type", String.class));

        if (requireRole != null) {
            String userType = claims.get("type", String.class);
            boolean hasRole = false;
            for (var role : requireRole.value()) {
                if (role.name().equals(userType)) {
                    hasRole = true;
                    break;
                }
            }
            if (!hasRole) {
                throw new BusinessException(4103, "无权限访问");
            }
        }

        return true;
    }
}
