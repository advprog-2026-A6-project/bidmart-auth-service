package id.ac.ui.cs.advprog.bidmartauthservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InternalServiceTokenFilterTest {

    @Test
    void allowsInternalRequestWhenTokenMatches() throws ServletException, IOException {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter("test-internal-token");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/users/1/public-profile");
        request.setServletPath("/api/internal/users/1/public-profile");
        request.addHeader(InternalServiceTokenFilter.HEADER_NAME, "test-internal-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void rejectsInternalRequestWhenTokenIsMissing() throws ServletException, IOException {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter("test-internal-token");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/users/1/contact-preferences");
        request.setServletPath("/api/internal/users/1/contact-preferences");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertEquals(403, response.getStatus());
    }

    @Test
    void skipsNonInternalRequests() throws ServletException, IOException {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter("test-internal-token");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/profile");
        request.setServletPath("/api/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
