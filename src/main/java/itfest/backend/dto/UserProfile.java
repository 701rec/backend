package itfest.backend.dto;

import itfest.backend.model.University;
import lombok.Builder;
import lombok.Data;

import java.util.List;

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