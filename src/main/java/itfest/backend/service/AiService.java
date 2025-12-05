package itfest.backend.service;

import itfest.backend.client.GeminiClient;
import itfest.backend.exception.ResourceNotFoundException;
import itfest.backend.model.AiRequest;
import itfest.backend.model.User;
import itfest.backend.repository.AiRequestRepository;
import itfest.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiRequestRepository aiRequestRepository;
    private final UserRepository userRepository;
    private final GeminiClient geminiClient;

    public String getAnswer(Long userId, String userPrompt) {
        String aiText = geminiClient.generateText(userPrompt);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с id " + userId + " не найден"));

        AiRequest historyItem = AiRequest.builder()
                .user(user)
                .inputText(userPrompt)
                .aiResponse(aiText)
                .build();

        aiRequestRepository.save(historyItem);

        return aiText;
    }
}