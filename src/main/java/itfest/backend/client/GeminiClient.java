package itfest.backend.client;

import itfest.backend.dto.gemini.GeminiRequest;
import itfest.backend.dto.gemini.GeminiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Импорт для логов
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j // Автоматически создает переменную log
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final RestClient restClient;

    @Value("${app.gemini-api-key}")
    private String apiKey;

    // Вынесли модель в переменную (можно будет менять через конфиг)
    private static final String MODEL_ID = "gemini-2.0-flash";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    public String generateChat(List<GeminiRequest.Content> chatHistory) {
        try {
            GeminiRequest requestBody = new GeminiRequest(chatHistory);

            GeminiResponse response = restClient.post()
                    .uri(BASE_URL + MODEL_ID + ":generateContent?key=" + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response != null && !response.candidates().isEmpty()) {
                return response.candidates().get(0).content().parts().get(0).text();
            }
            return "Не удалось получить ответ от AI (пустой ответ).";

        } catch (Exception e) {
            log.error("Ошибка при запросе к Gemini API: {}", e.getMessage());
            return "Извините, сервис временно недоступен.";
        }
    }
}