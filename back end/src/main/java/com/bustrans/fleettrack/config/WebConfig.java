package com.bustrans.fleettrack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String frontendPath;

    public WebConfig(@Value("${app.frontend-path}") String frontendPath) {
        this.frontendPath = frontendPath;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/*.html", "/*.png", "/css/**", "/js/**")
            .addResourceLocations(frontendPath, "file:d:/campus travelling system/stitch_transport_profile_interface/", "classpath:/static/");
        registry.addResourceHandler("/stitch_transport_profile_interface/**")
            .addResourceLocations("file:d:/campus travelling system/stitch_transport_profile_interface/");
    }
}
