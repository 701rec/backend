package itfest.backend.controller;

import itfest.backend.dto.AiChatRequest;
import itfest.backend.dto.GrantPrediction;
import itfest.backend.dto.GrantRequest;
import itfest.backend.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody AiChatRequest request) {
        String answer = aiService.getAnswer(request.getUserId(), request.getPrompt());

        return ResponseEntity.ok(Map.of("result", answer));
    }

    @PostMapping("/grant")
    public ResponseEntity<GrantPrediction> calculateGrantChance(@RequestBody GrantRequest request) {
        GrantPrediction prediction = aiService.calculateGrantChance(request.getScore(), request.getPreferredMajor());
        return ResponseEntity.ok(prediction);
    }
}
