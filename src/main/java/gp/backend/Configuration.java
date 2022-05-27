package gp.backend;

import gp.backend.auth.ApiClient;
import gp.backend.auth.auth.HttpBearerAuth;
import gp.backend.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;

@org.springframework.context.annotation.Configuration
@EnableScheduling
@OpenAPIDefinition(info = @Info(title = "My API", version = "v1"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class Configuration {

    private final ApiClient authApi;
    private final JwtTokenProvider jwtTokenProvider;

    @PostConstruct
    public void attachJWT() {
        HttpBearerAuth bearerAuth = (HttpBearerAuth) authApi.getAuthentication("bearerAuth");
        bearerAuth.setBearerToken(jwtTokenProvider.generateAdminToken());
    }


    @Scheduled(fixedRate = 1800000)
    public void scheduleFixedDelayTask() {
        HttpBearerAuth bearerAuth = (HttpBearerAuth) authApi.getAuthentication("bearerAuth");
        bearerAuth.setBearerToken(jwtTokenProvider.generateAdminToken());
    }

}
