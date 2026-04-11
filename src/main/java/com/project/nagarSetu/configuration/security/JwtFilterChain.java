package com.project.nagarSetu.configuration.security;

import com.project.nagarSetu.service.authenication.JwtService;
import com.project.nagarSetu.service.authenication.UserDetailService;
import com.project.nagarSetu.util.dto.authentication.TokenInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Configuration
@AllArgsConstructor
public class JwtFilterChain extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailService userDetailsService;

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String url = request.getContextPath();

        if(url.startsWith("/api/authenication") || url.startsWith("/v3/") || url.startsWith("/swagger-ui")){
            filterChain.doFilter(request , response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        TokenInfo tokenInfo = jwtService.extractClaim(token);
        UUID userId = UUID.fromString(tokenInfo.getId());

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetail = userDetailsService.loadUserById(userId);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetail, null, userDetail.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
