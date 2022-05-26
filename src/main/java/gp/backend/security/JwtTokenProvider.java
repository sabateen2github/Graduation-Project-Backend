package gp.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    /**
     * THIS IS NOT A SECURE PRACTICE! For simplicity, we are storing a static key here. Ideally, in a
     * microservices environment, this key would be kept on a config-server.
     */
    @Value("${security.jwt.token.secret-key:secret-key}")
    private String secretKey;

    @Value("${security.jwt.token.username}")
    private String username;

    @Value("${security.jwt.token.expire-length:3600000}")
    private long validityInMilliseconds = 3600000; // 1h


    private String adminWebToken = "invalid";

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }


    public Authentication getAuthentication(String token) {
        return new UsernamePasswordAuthenticationToken(getUsername(token), getInstituteId(token), getRoles(token));
    }

    private String getUsername(String token) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
    }

    private String getInstituteId(String token) {
        return (String) Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().get("instituteId");
    }

    public synchronized String generateAdminToken() {

        if (validateToken(adminWebToken)) return adminWebToken;

        Claims claims = Jwts.claims().setSubject(username);
        claims.put("auth", Arrays.asList(new SimpleGrantedAuthority(AppUserRole.ROLE_ADMIN.getAuthority())));
        claims.put("instituteId", "admin");
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        adminWebToken = Jwts.builder()//
                .setClaims(claims)//
                .setIssuedAt(now)//
                .setExpiration(validity)//
                .signWith(SignatureAlgorithm.HS256, secretKey)//
                .compact();

        return adminWebToken;
    }

    private List<AppUserRole> getRoles(String token) {

        List<String> roles = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().get("auth", List.class);
        return roles.stream().map(item -> {
            try {
                return AppUserRole.valueOf(item);
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Date now = new Date();
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getExpiration().after(new Date(now.getTime() - 30000)); //30 seconds error margin
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

}
