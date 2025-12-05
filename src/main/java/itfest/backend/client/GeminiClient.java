package itfest.backend.client;

import itfest.backend.dto.gemini.GeminiRequest;
import itfest.backend.dto.gemini.GeminiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final RestClient restClient;

    @Value("${app.gemini-api-key}")
    private String apiKey;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    public String generateText(String prompt) {
        try {
            GeminiRequest requestBody = new GeminiRequest(
                    List.of(new GeminiRequest.Content(
                            List.of(new GeminiRequest.Part(prompt))
                    ))
            );

            GeminiResponse response = restClient.post()
                    .uri(GEMINI_URL + "?key=" + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response != null && !response.candidates().isEmpty()) {
                return response.candidates().get(0).content().parts().get(0).text();
            }
            return "Не удалось получить ответ от AI.";

        } catch (Exception e) {
            System.err.println("Ошибка Gemini API: " + e.getMessage());
            return "Сервис временно недоступен. Попробуйте позже.";
        }
    }
}