package com.cps.backend.common.config;

import com.cps.backend.common.security.JwtAuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtInterceptor;

    @Value("${user.dir}")
    private String userDir;

    public WebMvcConfig(JwtAuthenticationInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/images/**");
    }

    /**
     * 配置静态资源映射：题目图片访问路径
     * 参考 02-Data-Dictionary.md §4.2.2 img 路径匹配规则
     * 将 /api/v1/images/** 映射到 Data/img/ 目录
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 图片路径：user.dir 是 backend/ 目录，../Data/img/ 即 Data/img/
        String imgPath = "file:" + userDir + "/../Data/img/";
        registry.addResourceHandler("/api/v1/images/**")
                .addResourceLocations(imgPath)
                .setCachePeriod(3600); // 缓存1小时
    }
}
