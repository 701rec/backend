package itfest.backend.dto;

import lombok.Data;

@Data
public class GrantRequest {
    private Integer score;
    private String preferredMajor;
}