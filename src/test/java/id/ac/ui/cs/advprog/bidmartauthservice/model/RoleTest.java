package id.ac.ui.cs.advprog.bidmartauthservice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void testEqualsAndHashCode() {
        Role role1 = Role.builder().id(1L).name("USER").build();
        Role role2 = Role.builder().id(1L).name("ADMIN").build();
        Role role3 = Role.builder().id(2L).name("USER").build();

        assertThat(role1).isEqualTo(role2);
        assertThat(role1).isNotEqualTo(role3);
        assertThat(role1).isNotEqualTo(null);
        assertThat(role1).isNotEqualTo(new Object());
        assertThat(role1.hashCode()).isEqualTo(role2.hashCode());
    }
}