package id.ac.ui.cs.advprog.bidmartauthservice.config;

import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserSessionRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        final String sessionTokenId;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwt = authHeader.substring(7);
            userEmail = jwtService.extractUsername(jwt);
            sessionTokenId = jwtService.extractSessionTokenId(jwt);
        } catch (RuntimeException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token tidak valid.");
            return;
        }

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));

            if (sessionTokenId == null || sessionTokenId.isBlank()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token sesi tidak ditemukan.");
                return;
            }

            if (jwtService.isTokenValid(jwt, userDetails)) {
                User user = (User) userDetails;
                var sessionOpt = userSessionRepository.findBySessionTokenId(sessionTokenId);
                boolean isSessionActive = sessionOpt.isPresent()
                        && sessionOpt.get().isActive()
                        && sessionOpt.get().getUser().getId().equals(user.getId());

                if (isSessionActive) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Sesi telah berakhir atau dicabut dari perangkat lain.");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
