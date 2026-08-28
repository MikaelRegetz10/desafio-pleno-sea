package com.desafio.sea.infra.security;

import com.desafio.sea.domain.User;
import com.desafio.sea.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = recoverToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = tokenService.validateToken(token);

        if (email.isBlank()) {
            handleUnauthorizedResponse(response, "Invalid or expired JWT token.");
            return;
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            handleUnauthorizedResponse(response, "User account not found.");
            return;
        }

        if (!user.isEnabled()) {
            handleUnauthorizedResponse(response, "User account is disabled.");
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    private void handleUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                message
        );
        problemDetail.setTitle("Unauthorized");
        problemDetail.setType(URI.create("https://api.sea.com/errors/unauthorized"));
        problemDetail.setProperty("timestamp", Instant.now());

        ObjectMapper mapper = new ObjectMapper();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(mapper.writeValueAsString(problemDetail));
    }
}
