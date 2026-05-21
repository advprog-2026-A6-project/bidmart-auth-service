package id.ac.ui.cs.advprog.bidmartauthservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class InternalServiceTokenFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Service-Token";
    private static final String INTERNAL_ROLE = "ROLE_INTERNAL_SERVICE";
    private static final String INTERNAL_PATH_PREFIX = "/api/internal/users/";

    private final String internalServiceToken;

    public InternalServiceTokenFilter(
            @Value("${auth.internal.service-token:}") String internalServiceToken
    ) {
        this.internalServiceToken = internalServiceToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (internalServiceToken == null || internalServiceToken.isBlank()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Internal service token is not configured");
            return;
        }

        String providedToken = request.getHeader(HEADER_NAME);
        if (!internalServiceToken.equals(providedToken)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Invalid internal service token");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "internal-service",
                null,
                List.of(new SimpleGrantedAuthority(INTERNAL_ROLE))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
