package com.yoichitgy.microservices.composite.product;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class ProductCompositeServiceConfiguration {
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }


    @Value("${api.common.version}")         String apiVersion;
    @Value("${api.common.title}")           String apiTitle;
    @Value("${api.common.description}")     String apiDescription;
    @Value("${api.common.termsOfService}")  String apiTermsOfService;
    @Value("${api.common.license}")         String apiLicense;
    @Value("${api.common.licenseUrl}")      String apiLicenseUrl;
    @Value("${api.common.externalDocDesc}") String apiExternalDocDesc;
    @Value("${api.common.externalDocUrl}")  String apiExternalDocUrl;
    @Value("${api.common.contact.name}")    String apiContactName;
    @Value("${api.common.contact.url}")     String apiContactUrl;
    @Value("${api.common.contact.email}")   String apiContactEmail;  

    @Bean
    public OpenAPI getOpenApiDocumentation() {
        var contact = new Contact()
            .name(apiContactName)
            .url(apiContactUrl)
            .email(apiContactEmail);
        var license = new License()
            .name(apiLicense)
            .url(apiLicenseUrl);
        var info = new Info()
            .title(apiTitle)
            .description(apiDescription)
            .version(apiVersion)
            .contact(contact)
            .termsOfService(apiTermsOfService)
            .license(license);
        var exernalDocs = new ExternalDocumentation()
            .description(apiExternalDocDesc)
            .url(apiExternalDocUrl);            

        return new OpenAPI()
            .info(info)
            .externalDocs(exernalDocs);
    }
}
