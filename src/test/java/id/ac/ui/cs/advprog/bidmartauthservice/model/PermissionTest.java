package id.ac.ui.cs.advprog.bidmartauthservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionTest {

    private Permission permission;

    @BeforeEach
    void setUp() {
        permission = Permission.builder()
                .id(1L)
                .name("READ_PRIVILEGE")
                .build();
    }

    @Test
    void testPermissionGetters() {
        assertEquals(1L, permission.getId());
        assertEquals("READ_PRIVILEGE", permission.getName());
    }

    @Test
    void testPermissionSetters() {
        Permission emptyPermission = new Permission();
        emptyPermission.setId(2L);
        emptyPermission.setName("WRITE_PRIVILEGE");

        assertEquals(2L, emptyPermission.getId());
        assertEquals("WRITE_PRIVILEGE", emptyPermission.getName());
    }
}