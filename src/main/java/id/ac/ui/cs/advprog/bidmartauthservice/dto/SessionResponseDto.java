package id.ac.ui.cs.advprog.bidmartauthservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionResponseDto {
    private Long id;
    private String sessionTokenId;
    private String deviceId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean active;
}
