package itfest.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import itfest.backend.client.GeminiClient;
import itfest.backend.dto.GrantPrediction;
import itfest.backend.dto.gemini.GeminiRequest;
import itfest.backend.exception.ResourceNotFoundException;
import itfest.backend.model.AiRequest;
import itfest.backend.model.University;
import itfest.backend.model.User;
import itfest.backend.repository.AiRequestRepository;
import itfest.backend.repository.UniversityRepository;
import itfest.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiRequestRepository aiRequestRepository;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Роль: Ты — консультант по поступлению (IT Fest).
            Имя студента: %s. Город: %s. ЕНТ: %s.
            
            Информация из базы данных (используй ТОЛЬКО её для фактов о ценах и программах):
            %s
            
            Отвечай кратко, дружелюбно и по делу.
            """;

    public String getAnswer(Long userId, String userPrompt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        List<String> keywords = Arrays.stream(userPrompt.split("\\s+"))
                .map(s -> s.replaceAll("[^a-zA-Zа-яА-Я0-9]", "")) // убираем знаки препинания
                .filter(s -> s.length() > 3)
                .collect(Collectors.toList());

        List<University> relevantUniversities;

        if (keywords.isEmpty() || isGreeting(userPrompt)) {
            relevantUniversities = new ArrayList<>();
        } else {
            Specification<University> spec = UniversitySpecification.searchByKeywords(keywords);
            relevantUniversities = universityRepository.findAll(spec, PageRequest.of(0, 5)).getContent();
        }

        if (relevantUniversities.isEmpty() && !isGreeting(userPrompt)) {
            relevantUniversities = universityRepository.findTopRated(PageRequest.of(0, 3));
        }

        String dbContext = formatUniversityData(relevantUniversities);
        String systemInstruction = String.format(SYSTEM_PROMPT_TEMPLATE,
                user.getFirstName(),
                user.getLocation() != null ? user.getLocation() : "Неизвестно",
                user.getEntScore() != null ? user.getEntScore() : "Нет",
                dbContext);

        List<GeminiRequest.Content> conversation = new ArrayList<>();
        conversation.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(systemInstruction))));
        conversation.add(new GeminiRequest.Content("model", List.of(new GeminiRequest.Part("Понял. Готов отвечать."))));

        List<AiRequest> history = aiRequestRepository.findLastRequests(userId, PageRequest.of(0, 4));
        Collections.reverse(history);
        for (AiRequest req : history) {
            conversation.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(req.getInputText()))));
            conversation.add(new GeminiRequest.Content("model", List.of(new GeminiRequest.Part(req.getAiResponse()))));
        }

        conversation.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(userPrompt))));

        String aiText = geminiClient.generateChat(conversation);

        saveRequest(user, userPrompt, aiText);

        return aiText;
    }

    public GrantPrediction calculateGrantChance(Integer score, String major) {
        if (score == null) score = 0;

        String targetMajor = (major != null && !major.isEmpty()) ? major : "IT";

        String prompt = String.format("""
                Роль: Аналитик данных ЕНТ (Казахстан, 2025).
                Задача: Рассчитать вероятность гранта (0-100%%) на основе статистики.
                
                Вводные:
                - Балл: %d / 140
                - Направление: %s
                
                Логика расчета (используй эти диапазоны):
                
                1. ЕСЛИ "Медицина", "Стоматология", "Право":
                   - < 100 баллов: шанс < 10%% (практически нет).
                   - 100-115 баллов: шанс 30-55%% (рискованно, только регионы).
                   - 116-125 баллов: шанс 60-85%% (хороший шанс).
                   - > 125 баллов: шанс 90-99%% (отлично).
                
                2. ЕСЛИ "IT", "Инженерия", "Педагогика":
                   - < 70 баллов: шанс < 15%%.
                   - 70-90 баллов: шанс 40-60%% (региональные вузы).
                   - 91-105 баллов: шанс 70-85%% (хороший уровень).
                   - > 105 баллов: шанс 90-99%% (топовые вузы).
                
                3. ЕСЛИ "Сельское хозяйство", "Ветеринария", "Биология":
                   - > 75 баллов: уже высокий шанс (>75%%).
                
                Формат ответа (JSON):
                {
                  "percentage": (число),
                  "comment": "Совет (макс 12 слов). Например: 'Для Алматы мало, пробуйте вузы Караганды или Семея'."
                }
                """, score, targetMajor);

        try {
            List<GeminiRequest.Content> content = List.of(
                    new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(prompt)))
            );

            String jsonResponse = geminiClient.generateChat(content);
            jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(jsonResponse, GrantPrediction.class);

        } catch (Exception e) {
            log.error("AI Error: {}", e.getMessage());
            return new GrantPrediction(0, "Ошибка расчета.");
        }
    }

    private void saveRequest(User user, String prompt, String response) {
        AiRequest newRecord = AiRequest.builder()
                .user(user)
                .inputText(prompt)
                .aiResponse(response)
                .build();
        aiRequestRepository.save(newRecord);
    }

    private String formatUniversityData(List<University> universities) {
        if (universities.isEmpty()) return "Нет данных о конкретных вузах по этому запросу.";
        return universities.stream()
                .map(u -> String.format("- %s (%s). Цена: %s. Рейтинг: %s. Инфо: %s",
                        u.getName(), u.getShortName(), u.getPrice(), u.getRating(), u.getDescription().substring(0, Math.min(u.getDescription().length(), 100)) + "..."))
                .collect(Collectors.joining("\n"));
    }

    private boolean isGreeting(String text) {
        String s = text.toLowerCase().trim();
        return s.matches("^(привет|здравствуйте|hello|hi|салам).*");
    }
}