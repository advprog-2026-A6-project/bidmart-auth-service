package id.ac.ui.cs.advprog.bidmartauthservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContactPreferencesResponseDto {
    private Long userId;
    private String email;
    private String preferredContactMethod;
    private boolean emailNotificationsEnabled;
    private boolean pushNotificationsEnabled;
}
