package itfest.backend.controller;

import itfest.backend.model.University;
import itfest.backend.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/universities")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityRepository universityRepository;

    // GET /api/universities?search=IT
    @GetMapping
    public List<University> getAll(@RequestParam(required = false) String search) {
        if (search != null && !search.isEmpty()) {
            return universityRepository.search(search);
        }
        return universityRepository.findAll();
    }

    // GET /api/universities/1
    @GetMapping("/{id}")
    public ResponseEntity<University> getById(@PathVariable Long id) {
        return universityRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
