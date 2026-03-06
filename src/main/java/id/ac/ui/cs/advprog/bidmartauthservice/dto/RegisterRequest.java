package id.ac.ui.cs.advprog.bidmartauthservice.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
}