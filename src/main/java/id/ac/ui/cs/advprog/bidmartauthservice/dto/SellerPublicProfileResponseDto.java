package id.ac.ui.cs.advprog.bidmartauthservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPublicProfileResponseDto {
    private Long id;
    private String name;
    private String bio;
    private String profilePictureUrl;
}
