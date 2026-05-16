package id.ac.ui.cs.advprog.bidmartauthservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User user;
    private Set<Role> roles;

    @BeforeEach
    void setUp() {
        roles = new HashSet<>();
        roles.add(Role.builder()
                .id(1L)
                .name("BUYER")
                .permissions(new HashSet<>())
                .build());

        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("password123")
                .name("Tester")
                .emailVerified(true)
                .phoneNumber("08123456789")
                .address("Jl. Testing No. 1")
                .bio("I am a tester")
                .profilePictureUrl("https://example.com/pic.jpg")
                .isTwoFactorEnabled(true)
                .twoFactorMethod(TwoFactorMethod.TOTP)
                .twoFactorSecret("SECRET_KEY")
                .roles(roles)
                .build();
    }

    @Test
    void testUserGetters() {
        assertEquals(1L, user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("password123", user.getPassword());
        assertEquals("Tester", user.getName());
        assertTrue(user.isEmailVerified());
        assertEquals("08123456789", user.getPhoneNumber());
        assertEquals("Jl. Testing No. 1", user.getAddress());
        assertEquals("I am a tester", user.getBio());
        assertEquals("https://example.com/pic.jpg", user.getProfilePictureUrl());
        assertTrue(user.isTwoFactorEnabled());
        assertEquals(TwoFactorMethod.TOTP, user.getTwoFactorMethod());
        assertEquals("SECRET_KEY", user.getTwoFactorSecret());
        assertEquals(roles, user.getRoles());
    }

    @Test
    void testUserSetters() {
        User emptyUser = new User();

        Set<Role> newRoles = new HashSet<>();
        newRoles.add(Role.builder()
                .id(2L)
                .name("ADMIN")
                .permissions(new HashSet<>())
                .build());

        emptyUser.setId(2L);
        emptyUser.setEmail("new@example.com");
        emptyUser.setPassword("newpass");
        emptyUser.setName("New Tester");
        emptyUser.setEmailVerified(false);
        emptyUser.setPhoneNumber("08999999999");
        emptyUser.setAddress("Jl. Baru No. 2");
        emptyUser.setBio("New bio");
        emptyUser.setProfilePictureUrl("https://example.com/newpic.jpg");
        emptyUser.setTwoFactorEnabled(false);
        emptyUser.setTwoFactorMethod(TwoFactorMethod.EMAIL);
        emptyUser.setTwoFactorSecret("NEW_SECRET");
        emptyUser.setRoles(newRoles);

        assertEquals(2L, emptyUser.getId());
        assertEquals("new@example.com", emptyUser.getEmail());
        assertEquals("newpass", emptyUser.getPassword());
        assertEquals("New Tester", emptyUser.getName());
        assertFalse(emptyUser.isEmailVerified());
        assertEquals("08999999999", emptyUser.getPhoneNumber());
        assertEquals("Jl. Baru No. 2", emptyUser.getAddress());
        assertEquals("New bio", emptyUser.getBio());
        assertEquals("https://example.com/newpic.jpg", emptyUser.getProfilePictureUrl());
        assertFalse(emptyUser.isTwoFactorEnabled());
        assertEquals(TwoFactorMethod.EMAIL, emptyUser.getTwoFactorMethod());
        assertEquals("NEW_SECRET", emptyUser.getTwoFactorSecret());
        assertEquals(newRoles, emptyUser.getRoles());
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
        assertEquals("ROLE_BUYER", authorities.iterator().next().getAuthority());
    }

    @Test
    void testGetAuthoritiesWithNullRoles() {
        User userNullRoles = User.builder().email("test@mail.com").build();
        Collection<? extends GrantedAuthority> authorities = userNullRoles.getAuthorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.iterator().next().getAuthority());
    }
}
