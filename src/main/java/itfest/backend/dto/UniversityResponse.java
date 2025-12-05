package itfest.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UniversityResponse {
    private Long id;
    private String name;
    private String shortName;
    private String type;
    private String price;
    private Double rating;
    private String location;
    private String imageUrl;
    private String description;
    private String contacts;
    private String website;
    private Boolean military;
    private Boolean dorm;
    private String focus;
    private List<String> programs;
}