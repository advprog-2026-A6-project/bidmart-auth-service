package id.ac.ui.cs.advprog.bidmartauthservice.config;

import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.model.UserSession;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserSessionRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User dummyUser;
    private UserSession dummySession;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        dummyUser = User.builder().id(1L).email("test@example.com").password("pass").build();
        dummySession = UserSession.builder()
                .isActive(true)
                .deviceId("Device-Test")
                .sessionTokenId("session-123")
                .user(dummyUser)
                .build();
    }

    @Test
    void doFilterInternal_NoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_InvalidAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic 12345");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ValidTokenAndActiveSession() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtService.extractUsername("valid.token.here")).thenReturn("test@example.com");
        when(jwtService.extractSessionTokenId("valid.token.here")).thenReturn("session-123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(dummyUser));
        when(jwtService.isTokenValid("valid.token.here", dummyUser)).thenReturn(true);
        when(userSessionRepository.findBySessionTokenId("session-123"))
                .thenReturn(Optional.of(dummySession));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_InvalidOrExpiredToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.token.here");
        when(jwtService.extractUsername("expired.token.here")).thenReturn("test@example.com");
        when(jwtService.extractSessionTokenId("expired.token.here")).thenReturn("session-123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(dummyUser));
        when(jwtService.isTokenValid("expired.token.here", dummyUser)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ValidTokenButInactiveSession() throws Exception {
        dummySession.setActive(false);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtService.extractUsername("valid.token.here")).thenReturn("test@example.com");
        when(jwtService.extractSessionTokenId("valid.token.here")).thenReturn("session-123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(dummyUser));
        when(jwtService.isTokenValid("valid.token.here", dummyUser)).thenReturn(true);
        when(userSessionRepository.findBySessionTokenId("session-123"))
                .thenReturn(Optional.of(dummySession));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_DisabledUserRejected() throws Exception {
        dummyUser.setActive(false);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtService.extractUsername("valid.token.here")).thenReturn("test@example.com");
        when(jwtService.extractSessionTokenId("valid.token.here")).thenReturn("session-123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(dummyUser));
        when(jwtService.isTokenValid("valid.token.here", dummyUser)).thenReturn(true);
        when(userSessionRepository.findBySessionTokenId("session-123")).thenReturn(Optional.of(dummySession));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_InvalidTokenExtractionRejected() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer broken.token");
        when(jwtService.extractUsername("broken.token")).thenThrow(new RuntimeException("bad token"));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_MissingSessionTokenIdRejected() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtService.extractUsername("valid.token.here")).thenReturn("test@example.com");
        when(jwtService.extractSessionTokenId("valid.token.here")).thenReturn(" ");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(dummyUser));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_SessionOwnedByAnotherUserRejected() throws Exception {
        User otherUser = User.builder().id(2L).email("other@example.com").password("pass").build();
        dummySession.setUser(otherUser);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtService.extractUsername("valid.token.here")).thenReturn("test@example.com");
        when(jwtService.extractSessionTokenId("valid.token.here")).thenReturn("session-123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(dummyUser));
        when(jwtService.isTokenValid("valid.token.here", dummyUser)).thenReturn(true);
        when(userSessionRepository.findBySessionTokenId("session-123")).thenReturn(Optional.of(dummySession));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_SkipsWhenAuthenticationAlreadyPresent() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("existing", null)
        );
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtService.extractUsername("valid.token.here")).thenReturn("test@example.com");
        when(jwtService.extractSessionTokenId("valid.token.here")).thenReturn("session-123");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userRepository, never()).findByEmail(any());
    }
}
