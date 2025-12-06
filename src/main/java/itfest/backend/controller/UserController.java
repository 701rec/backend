package itfest.backend.controller;

import itfest.backend.dto.UserProfile;
import itfest.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfile> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getProfile(id));
    }
}