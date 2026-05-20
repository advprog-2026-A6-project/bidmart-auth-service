package id.ac.ui.cs.advprog.bidmartauthservice.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void testGetAuthorities_WithRolesAndPermissions() {
        Permission readPerm = Permission.builder().id(1L).name("wallet:read").build();
        Role userRole = Role.builder().id(1L).name("USER").permissions(Set.of(readPerm)).build();

        User user = User.builder()
                .id(100L)
                .email("test@mail.com")
                .roles(Set.of(userRole))
                .build();

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertThat(authorities).hasSize(2); 
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "wallet:read");
    }

    @Test
    void testGetAuthorities_EmptyRoles() {
        User user = User.builder()
                .id(100L)
                .email("test@mail.com")
                .roles(new HashSet<>())
                .build();

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertThat(authorities).hasSize(1);
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void testGetAuthorities_NullRoles() {
        User user = new User();
        user.setRoles(null);

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertThat(authorities).hasSize(1);
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void testEqualsAndHashCode() {
        User user1 = User.builder().id(1L).email("a@mail.com").build();
        User user2 = User.builder().id(1L).email("b@mail.com").build();
        User user3 = User.builder().id(2L).email("a@mail.com").build();

        assertThat(user1).isEqualTo(user2);
        assertThat(user1).isNotEqualTo(user3);
        assertThat(user1).isNotEqualTo(null);
        assertThat(user1).isNotEqualTo(new Object());
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    void testDefaultContactPreferences() {
        User user = User.builder()
                .id(10L)
                .email("contact@mail.com")
                .build();

        assertThat(user.getPreferredContactMethod()).isEqualTo(PreferredContactMethod.EMAIL);
        assertThat(user.isEmailNotificationsEnabled()).isTrue();
        assertThat(user.isPushNotificationsEnabled()).isFalse();
    }
}
