package itfest.backend.service;

import itfest.backend.dto.UserProfile;
import itfest.backend.exception.ResourceNotFoundException;
import itfest.backend.mapper.UniversityMapper;
import itfest.backend.model.User;
import itfest.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UniversityMapper universityMapper;

    @Transactional(readOnly = true)
    public UserProfile getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        return UserProfile.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName((user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : ""))
                .status(user.getStatus())
                .entScore(user.getEntScore())
                .location(user.getLocation())
                .avatarUrl(user.getAvatarUrl())
                .favorites(universityMapper.toDtoList(user.getFavorites()))
                .build();
    }
}