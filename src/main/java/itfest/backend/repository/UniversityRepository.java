package itfest.backend.repository;

import itfest.backend.model.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UniversityRepository extends JpaRepository<University, Long> {

    @Query("SELECT u FROM University u WHERE " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.shortName) LIKE LOWER(CONCAT('%', :query, '%'))" +
            "ORDER BY u.id DESC")
    List<University> search(@Param("query") String query);
}
