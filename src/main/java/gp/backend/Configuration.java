package gp.backend;

import gp.backend.auth.ApiClient;
import gp.backend.exception.ResponseErrorHandler;
import gp.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;

@org.springframework.context.annotation.Configuration
@EnableScheduling
@RequiredArgsConstructor
public class Configuration {

    private final ApiClient authApi;
    private final JwtTokenProvider jwtTokenProvider;

    @PostConstruct
    public void attachJWT() {
        authApi.addDefaultHeader("Authorization", "Bearer " + jwtTokenProvider.generateAdminToken());
    }


    @Scheduled(fixedRate = 1800000)
    public void scheduleFixedDelayTask() {
        authApi.addDefaultHeader("Authorization", "Bearer " + jwtTokenProvider.generateAdminToken());
    }

}
