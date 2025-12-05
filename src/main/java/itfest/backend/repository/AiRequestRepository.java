package itfest.backend.repository;

import itfest.backend.model.AiRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiRequestRepository extends JpaRepository<AiRequest, Long> {
    List<AiRequest> findByUserId(Long userId);
}
