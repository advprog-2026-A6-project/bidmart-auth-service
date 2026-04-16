package id.ac.ui.cs.advprog.bidmartauthservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponseDto {
    private Long id;
    private String email;
    private String name;
    private String phoneNumber;
    private String address;
    private String bio;
    private String profilePictureUrl;
    private boolean isTwoFactorEnabled;
}