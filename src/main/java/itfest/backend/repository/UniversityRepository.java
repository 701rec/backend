package itfest.backend.repository;

import itfest.backend.model.University;
import org.springframework.data.domain.Pageable; // Важно добавить этот импорт
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UniversityRepository extends JpaRepository<University, Long> {

    @Query("SELECT u FROM University u WHERE " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.shortName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.focus) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<University> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT u FROM University u ORDER BY u.rating DESC")
    List<University> findTopRated(Pageable pageable);
}