package itfest.backend.controller;

import itfest.backend.dto.UniversityResponse;
import itfest.backend.service.UniversityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/universities")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;

    @GetMapping
    public List<UniversityResponse> getAll(@RequestParam(required = false) String search) {
        return universityService.getAllUniversities(search);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UniversityResponse> getById(@PathVariable Long id) {
        return universityService.getUniversityById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}