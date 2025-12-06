package itfest.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "universities")
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String shortName;
    private String type;
    private String price;
    private Double rating;
    private String location;
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String contacts;
    private String website;

    private Boolean military;
    private Boolean dorm;
    private String focus;

    @ElementCollection
    @CollectionTable(name = "university_programs", joinColumns = @JoinColumn(name = "university_id"))
    @Column(name = "program_name")
    private List<String> programs;
}