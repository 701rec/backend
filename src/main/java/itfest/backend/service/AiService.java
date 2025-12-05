package itfest.backend.service;

import itfest.backend.dto.gemini.GeminiRequest;
import itfest.backend.dto.gemini.GeminiResponse;
import itfest.backend.model.AiRequest;
import itfest.backend.model.User;
import itfest.backend.repository.AiRequestRepository;
import itfest.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiRequestRepository aiRequestRepository;
    private final UserRepository userRepository;

    private final RestClient restClient = RestClient.create();

    @Value("${app.gemini-api-key}")
    private String apiKey;

    public String getAnswer(Long userId, String userPrompt) {
        GeminiRequest requestBody = new GeminiRequest(
                List.of(new GeminiRequest.Content(
                        List.of(new GeminiRequest.Part(userPrompt))
                ))
        );

        GeminiResponse response = restClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);

        String aiText = "Ошибка генерации";
        if (response != null && !response.candidates().isEmpty()) {
            aiText = response.candidates().get(0).content().parts().get(0).text();
        }

        User user = userRepository.findById(userId).orElseThrow();

        AiRequest historyItem = AiRequest.builder()
                .user(user)
                .inputText(userPrompt)
                .aiResponse(aiText)
                .build();

        aiRequestRepository.save(historyItem);

        return aiText;
    }
}
