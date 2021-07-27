/*
 * Copyright (c) 2020 - Department of Research & Development
 * General Lighthouse Authorities of the UK and Ireland's.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

package org.grad.eNav.msgBroker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The WebConfig Class
 *
 * This is the main configuration class for the Web MVC operations.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    /**
     * Add the static resources and webjars to the web resources.
     *
     * @param registry the resource handler registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/webjars/**",
                        "/css/**",
                        "/images/**",
                        "/src/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/",
                        "classpath:/static/css/",
                        "classpath:/static/images/",
                        "classpath:/static/src/")
                .resourceChain(false);
        registry.setOrder(1);
    }

    /**
     * Make the index.html our main page so that it's being picked up by
     * Thymeleaf.
     *
     * @param registry The View Controller Registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

}
