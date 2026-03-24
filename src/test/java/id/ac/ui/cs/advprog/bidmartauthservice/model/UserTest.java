package id.ac.ui.cs.advprog.bidmartauthservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("password123")
                .name("Tester")
                .phoneNumber("08123456789")
                .address("Jl. Testing No. 1")
                .bio("I am a tester")
                .profilePictureUrl("https://example.com/pic.jpg")
                .build();
    }

    @Test
    void testUserGetters() {
        assertEquals(1L, user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("password123", user.getPassword());
        assertEquals("Tester", user.getName());
        assertEquals("08123456789", user.getPhoneNumber());
        assertEquals("Jl. Testing No. 1", user.getAddress());
        assertEquals("I am a tester", user.getBio());
        assertEquals("https://example.com/pic.jpg", user.getProfilePictureUrl());
    }

    @Test
    void testUserSetters() {
        User emptyUser = new User();

        emptyUser.setId(2L);
        emptyUser.setEmail("new@example.com");
        emptyUser.setPassword("newpass");
        emptyUser.setName("New Tester");
        emptyUser.setPhoneNumber("08999999999");
        emptyUser.setAddress("Jl. Baru No. 2");
        emptyUser.setBio("New bio");
        emptyUser.setProfilePictureUrl("https://example.com/newpic.jpg");

        assertEquals(2L, emptyUser.getId());
        assertEquals("new@example.com", emptyUser.getEmail());
        assertEquals("newpass", emptyUser.getPassword());
        assertEquals("New Tester", emptyUser.getName());
        assertEquals("08999999999", emptyUser.getPhoneNumber());
        assertEquals("Jl. Baru No. 2", emptyUser.getAddress());
        assertEquals("New bio", emptyUser.getBio());
        assertEquals("https://example.com/newpic.jpg", emptyUser.getProfilePictureUrl());
    }

    @Test
    void testUserDetailsMethods() {
        assertEquals("test@example.com", user.getUsername());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.iterator().next().getAuthority());
    }
}