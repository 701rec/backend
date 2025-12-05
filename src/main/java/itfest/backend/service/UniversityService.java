package itfest.backend.service;

import itfest.backend.dto.UniversityResponse;
import itfest.backend.mapper.UniversityMapper;
import itfest.backend.model.University;
import itfest.backend.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UniversityService {

    private final UniversityRepository universityRepository;
    private final UniversityMapper universityMapper;

    public List<UniversityResponse> getAllUniversities(String search) {
        List<University> universities;
        if (search != null && !search.isEmpty()) {
            universities = universityRepository.search(search);
        } else {
            universities = universityRepository.findAll();
        }
        return universityMapper.toDtoList(universities);
    }

    public Optional<UniversityResponse> getUniversityById(Long id) {
        return universityRepository.findById(id)
                .map(universityMapper::toDto);
    }
}