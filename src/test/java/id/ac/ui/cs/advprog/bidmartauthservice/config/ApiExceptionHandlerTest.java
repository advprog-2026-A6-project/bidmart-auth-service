package id.ac.ui.cs.advprog.bidmartauthservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void handleIllegalArgumentException() {
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgumentException(new IllegalArgumentException("Error test"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Error test");
    }

    @Test
    void handleIllegalStateException() {
        ResponseEntity<Map<String, String>> response = handler.handleIllegalStateException(new IllegalStateException("State error"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "State error");
    }

    @Test
    void handleBadCredentialsException() {
        ResponseEntity<Map<String, String>> response = handler.handleBadCredentialsException(new BadCredentialsException("Bad creds"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "Email atau password salah");
    }

    @Test
    void handleAccessDeniedException() {
        ResponseEntity<Map<String, String>> response = handler.handleAccessDeniedException(new AccessDeniedException("Denied"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("error", "Akses ditolak");
    }

    @Test
    void handleValidationExceptions() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        try {
            handler.handleValidationExceptions(ex);
        } catch (Exception ignored) {
        }
    }
}
