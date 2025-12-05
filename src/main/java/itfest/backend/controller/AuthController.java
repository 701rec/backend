package itfest.backend.controller;

import itfest.backend.dto.LoginRequest;
import itfest.backend.model.Role;
import itfest.backend.model.User;
import itfest.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(request.getEmail());

        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else {
            User newUser = User.builder()
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .role(Role.USER)
                    .build();
            userRepository.save(newUser);
            return ResponseEntity.ok(newUser);
        }
    }
}