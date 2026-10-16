
package com.company.enroller.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.company.enroller.model.Participant;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

public class JWTAuthenticationFilter
        extends AbstractAuthenticationProcessingFilter {

    private final AuthenticationManager authenticationManager;
    private final String secret;
    private final String issuer;
    private final int tokenExpiration;

    public JWTAuthenticationFilter(
            AuthenticationManager authenticationManager,
            String secret,
            String issuer,
            int tokenExpiration) {

        super(new AntPathRequestMatcher(
                "/tokens", HttpMethod.POST.name()));

        this.authenticationManager = authenticationManager;
        this.secret = secret;
        this.issuer = issuer;
        this.tokenExpiration = tokenExpiration;
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest req,
            HttpServletResponse res)
            throws AuthenticationException {

        try {
            Participant participant = new ObjectMapper()
                    .readValue(req.getInputStream(), Participant.class);

            if (participant.getLogin() == null ||
                    participant.getPassword() == null) {
                throw new BadCredentialsException(
                        "Login and password are required");
            }

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            participant.getLogin(),
                            participant.getPassword());

            return authenticationManager.authenticate(authentication);

        } catch (IOException e) {
            throw new BadCredentialsException(
                    "Invalid login request.", e);
        }
    }

    @Override
    protected void successfulAuthentication(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain,
            Authentication auth) throws IOException {

        String login = ((UserDetails) auth.getPrincipal())
                .getUsername();

        Instant now = Instant.now();

        String token = JWT.create()
                .withIssuer(issuer)
                .withSubject(login)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(
                        Date.from(now.plusSeconds(tokenExpiration)))
                .withClaim("role", "participant")
                .sign(Algorithm.HMAC256(secret));

        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        res.getWriter().write(
                String.format("{\"token\":\"%s\"}", token));
    }
}
