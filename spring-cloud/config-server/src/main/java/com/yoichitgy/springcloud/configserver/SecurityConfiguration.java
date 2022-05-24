package com.yoichitgy.springcloud.configserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration /* extends WebSecurityConfigurerAdapter */ {
    // @Override
    // protected void configure(HttpSecurity http) throws Exception {
    //   http
    //     // Disable CRCF to allow POST to /encrypt and /decrypt endpoins
    //     .csrf()
    //       .disable()
    //     .authorizeRequests()
    //       .anyRequest().authenticated()
    //       .and()
    //       .httpBasic();
    // }  

    // https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter/#configuring-httpsecurity
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CRCF to allow services to register themselves with Eureka
            .csrf()
                .disable()
            .authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .httpBasic();
        return http.build();
    }
}
