package gp.backend;

import gp.backend.auth.ApiClient;
import gp.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@org.springframework.context.annotation.Configuration
@EnableScheduling
@RequiredArgsConstructor
public class Configuration {

    private final ResponseErrorHandler responseErrorHandler;
    private final ApiClient authApi;
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public RestTemplate getRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.errorHandler(responseErrorHandler).build();
    }

    @PostConstruct
    public void attachJWT() {
        authApi.addDefaultHeader("Authorization", "Bearer " + jwtTokenProvider.generateAdminToken());
    }


    @Scheduled(fixedRate = 1800000)
    public void scheduleFixedDelayTask() {
        authApi.addDefaultHeader("Authorization", "Bearer " + jwtTokenProvider.generateAdminToken());
    }

}
