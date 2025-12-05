package itfest.backend.repository;

import itfest.backend.model.AiRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiRequestRepository extends JpaRepository<AiRequest, Long> {

    @Query("SELECT r FROM AiRequest r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<AiRequest> findLastRequests(@Param("userId") Long userId, Pageable pageable);
}