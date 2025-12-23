package com.saaspos.api.config;

import com.saaspos.api.service.CustomUserDetailsService;
import com.saaspos.api.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Buscar el header "Authorization"
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. Si no hay header o no empieza con "Bearer ", dejar pasar (Spring Security rechazará después si es necesario)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraer el token
        jwt = authHeader.substring(7);

        // 4. Extraer el email del token
        try {
            userEmail = jwtUtil.extractUsername(jwt);
            System.out.println("JWT email: " + userEmail);
        } catch (Exception e) {
            // Si el token está corrupto, seguimos sin autenticar
            System.out.println("Error extrayendo username del JWT: " + e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Si hay email y el usuario no está autenticado todavía en el contexto
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Cargar detalles del usuario desde la DB
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 6. Validar token
            if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                // Crear objeto de autenticación
                System.out.println("JWT válido para: " + userDetails.getUsername());
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. ESTABLECER LA SEGURIDAD (Aquí es donde Spring se entera de que estás logueado)
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                System.out.println("JWT inválido");
            }
        }

        // 8. Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}