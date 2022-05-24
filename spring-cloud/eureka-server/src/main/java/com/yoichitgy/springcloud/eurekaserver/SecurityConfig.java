package com.yoichitgy.springcloud.eurekaserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
//public class SecurityConfig extends WebSecurityConfigurerAdapter {
public class SecurityConfig {
    private final String username;
    private final String password;

    @Autowired
    public SecurityConfig(
        @Value("${app.eureka-username}") String username,
        @Value("${app.eureka-password}") String password
    ) {
        this.username = username;
        this.password = password;
    }

    // @Override
    // public void configure(AuthenticationManagerBuilder auth) throws Exception {
    //     auth.inMemoryAuthentication()
    //         .passwordEncoder(NoOpPasswordEncoder.getInstance())
    //         .withUser(username).password(password)
    //         .authorities("USER");
    // }
    
    // @Override
    // protected void configure(HttpSecurity http) throws Exception {
    //     http
    //         // Disable CRCF to allow services to register themselves with Eureka
    //         .csrf()
    //             .disable()
    //         .authorizeRequests()
    //             .anyRequest().authenticated()
    //             .and()
    //             .httpBasic();
    // }

    // https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter/#in-memory-authentication
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        var user = User.builder()
            .passwordEncoder(encoder::encode)
            .username(username)
            .password(password)
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }

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
