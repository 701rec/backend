package itfest.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfile {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String status;
    private Integer entScore;
    private String location;
    private String avatarUrl;
}