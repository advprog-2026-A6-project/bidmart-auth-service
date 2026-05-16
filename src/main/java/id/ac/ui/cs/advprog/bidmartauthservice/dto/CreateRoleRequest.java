package id.ac.ui.cs.advprog.bidmartauthservice.dto;

import lombok.Data;

import java.util.Set;

@Data
public class CreateRoleRequest {
    private String name;
    private Set<String> permissions;
}
