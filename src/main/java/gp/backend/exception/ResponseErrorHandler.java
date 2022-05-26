package gp.backend.exception;

import gp.backend.auth.ApiClient;
import gp.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ResponseErrorHandler implements org.springframework.web.client.ResponseErrorHandler {

    private final ApiClient authApi;
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {

        if (response.getStatusCode().is2xxSuccessful()) return false;
        return true;
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            authApi.addDefaultHeader("Authorization", "Bearer " + jwtTokenProvider.generateAdminToken());
        }
    }
}
