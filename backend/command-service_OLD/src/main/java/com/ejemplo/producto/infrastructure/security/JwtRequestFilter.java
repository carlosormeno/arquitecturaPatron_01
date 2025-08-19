package com.ejemplo.producto.infrastructure.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {


        String uri = request.getRequestURI();
        String method = request.getMethod();

        // ✅ PERMITIR TODAS las requests OPTIONS inmediatamente
        if ("OPTIONS".equalsIgnoreCase(method)) {
            logger.debug("✅ Allowing OPTIONS request for CORS: {}", uri);
            filterChain.doFilter(request, response);
            return;
        }

        // 🔓 Excluir rutas que no requieren autenticación
        if (shouldSkipAuthentication(uri)) {
            logger.debug("✅ Skipping authentication for URI: {}", uri);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        logger.info("🔐 JWT Filter ejecutado para URI: {} {}", method, uri);
        logger.info("🔑 Authorization header presente: {}", authHeader != null ? "SÍ" : "NO");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("❌ Falta token o formato inválido para URI: {}", uri);
            logger.warn("❌ Header recibido: [{}]", authHeader);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid token\",\"uri\":\"" + uri + "\"}");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Falta token o formato inválido");
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            logger.warn("❌ Token inválido para URI: {}", uri);

            String username = jwtUtil.extractUsername(token);
            logger.warn("❌ Username extraído del token inválido: {}", username);

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid token\",\"uri\":\"" + uri + "\"}");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
            return;
        }

        String username = jwtUtil.extractUsername(token);
        logger.info("✅ Token válido para usuario: {}", username);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Extraer roles del token si existen
            List<String> roles = jwtUtil.extractRoles(token);
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            if (roles != null && !roles.isEmpty()) {
                authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
                logger.info("✅ Roles extraídos del token: {}", roles);
            } else {
                // Si no hay roles, asignar rol USER por defecto
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                logger.info("⚠️ No se encontraron roles en el token, asignando ROLE_USER por defecto");
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username, null, new ArrayList<>()); // puedes añadir roles aquí si lo deseas
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.info(">>> Usuario autenticado en SecurityContext: " + username);
        }

        // Si el token es válido, continúa
        logger.debug("✅ Continuando con la cadena de filtros para URI: {}", uri);
        filterChain.doFilter(request, response);

    }

    private boolean shouldSkipAuthentication(String uri) {
        return uri.startsWith("/api/auth/login") ||
                uri.startsWith("/api/auth/ping") ||
                uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/swagger") ||
                uri.startsWith("/actuator") ||
                uri.startsWith("/favicon.ico") ||
                uri.contains("/otel") ||
                uri.contains("4318") ||
                uri.equals("/") ||
                uri.startsWith("/error");
    }

}
