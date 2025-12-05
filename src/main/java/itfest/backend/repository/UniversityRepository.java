package itfest.backend.repository;

import itfest.backend.model.University;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UniversityRepository extends JpaRepository<University, Long>, JpaSpecificationExecutor<University> {

    @Query("SELECT u FROM University u ORDER BY u.rating DESC")
    List<University> findTopRated(Pageable pageable);
}