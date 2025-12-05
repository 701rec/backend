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

    private String name;        // "International IT University"
    private String shortName;   // "IITU (МУИТ)"
    private String type;        // "IT & Инжиниринг"
    private String price;       // "1.2 млн ₸" (храним строкой для простоты)
    private Double rating;      // 4.9
    private String location;    // "Алматы"
    private String imageUrl;    // Ссылка на фото

    @Column(columnDefinition = "TEXT")
    private String description; // Полное описание

    private String contacts;    // "+7 (727)..."
    private String website;     // "www.iitu.kz"

    private Boolean military;   // Военная кафедра (true/false)
    private Boolean dorm;       // Общежитие (true/false)
    private String focus;       // Профиль (например "IT")

    // Список программ (Computer Science, и т.д.)
    @ElementCollection
    @CollectionTable(name = "university_programs", joinColumns = @JoinColumn(name = "university_id"))
    @Column(name = "program_name")
    private List<String> programs;
}