package gp.backend.service;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.TravelMode;
import gp.backend.dto.LatLng;
import gp.backend.security.DurationServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class DistanceService {
    @Value("${google.maps.api-key}")
    private String apiKey;

    public long getDurationInSeconds(LatLng origin, LatLng destination) throws DurationServiceException {
        GeoApiContext context = new GeoApiContext.Builder().apiKey(apiKey).build();
        try {
            DistanceMatrix distanceMatrix = DistanceMatrixApi.newRequest(context).origins(new com.google.maps.model.LatLng(origin.getLat(), origin.getLng())).destinations(new com.google.maps.model.LatLng(destination.getLat(), destination.getLng())).mode(TravelMode.DRIVING).await();
            long duration = distanceMatrix.rows[0].elements[0].durationInTraffic.inSeconds;
            return duration;
        } catch (ApiException | InterruptedException | IOException e) {
            e.printStackTrace();
            throw new DurationServiceException(e);
        }
    }
}
