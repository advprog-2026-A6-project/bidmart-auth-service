package id.ac.ui.cs.advprog.bidmartauthservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
    void handleDisabledAccount() {
        ResponseEntity<Map<String, String>> response = handler.handleDisabledAccount(new DisabledException("Akun dinonaktifkan"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("error", "Akun dinonaktifkan");
    }

    @Test
    void handleUsernameNotFound() {
        ResponseEntity<Map<String, String>> response = handler.handleUsernameNotFound(new UsernameNotFoundException("User tidak ditemukan"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "User tidak ditemukan");
    }

    @Test
    void handleValidationExceptions() throws Exception {
        DummyRequest request = new DummyRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "dummyRequest");
        bindingResult.rejectValue("email", "NotBlank", "must not be blank");

        Method method = DummyValidationController.class.getDeclaredMethod("submit", DummyRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Validation failed");
        assertThat(response.getBody()).containsKey("details");
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) response.getBody().get("details");
        assertThat(details).containsEntry("email", "must not be blank");
    }

    static class DummyValidationController {
        void submit(@RequestBody DummyRequest request) {
        }
    }

    static class DummyRequest {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
