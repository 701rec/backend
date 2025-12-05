package itfest.backend.mapper;

import itfest.backend.dto.UniversityResponse;
import itfest.backend.model.University;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UniversityMapper {

    public UniversityResponse toDto(University university) {
        if (university == null) {
            return null;
        }

        return UniversityResponse.builder()
                .id(university.getId())
                .name(university.getName())
                .shortName(university.getShortName())
                .type(university.getType())
                .price(university.getPrice())
                .rating(university.getRating())
                .location(university.getLocation())
                .imageUrl(university.getImageUrl())
                .description(university.getDescription())
                .contacts(university.getContacts())
                .website(university.getWebsite())
                .military(university.getMilitary())
                .dorm(university.getDorm())
                .focus(university.getFocus())
                .programs(university.getPrograms() != null ? List.copyOf(university.getPrograms()) : List.of())
                .build();
    }

    public List<UniversityResponse> toDtoList(List<University> universities) {
        return universities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}