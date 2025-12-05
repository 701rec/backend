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

        // 1. Очистка и подготовка ключевых слов (берем слова длиннее 3 букв)
        List<String> keywords = Arrays.stream(userPrompt.split("\\s+"))
                .map(s -> s.replaceAll("[^a-zA-Zа-яА-Я0-9]", "")) // убираем знаки препинания
                .filter(s -> s.length() > 3)
                .collect(Collectors.toList());

        List<University> relevantUniversities;

        // 2. Умный поиск (ОДИН ЗАПРОС вместо цикла)
        if (keywords.isEmpty() || isGreeting(userPrompt)) {
            relevantUniversities = new ArrayList<>();
        } else {
            Specification<University> spec = UniversitySpecification.searchByKeywords(keywords);
            // Ищем вузы по спецификации, берем первые 5 совпадений
            relevantUniversities = universityRepository.findAll(spec, PageRequest.of(0, 5)).getContent();
        }

        // Если ничего не нашли, но вопрос про поступление - подкинем топовые вузы
        if (relevantUniversities.isEmpty() && !isGreeting(userPrompt)) {
            relevantUniversities = universityRepository.findTopRated(PageRequest.of(0, 3));
        }

        // 3. Формирование контекста
        String dbContext = formatUniversityData(relevantUniversities);
        String systemInstruction = String.format(SYSTEM_PROMPT_TEMPLATE,
                user.getFirstName(),
                user.getLocation() != null ? user.getLocation() : "Неизвестно",
                user.getEntScore() != null ? user.getEntScore() : "Нет",
                dbContext);

        // 4. Сборка истории чата
        List<GeminiRequest.Content> conversation = new ArrayList<>();
        conversation.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(systemInstruction))));
        conversation.add(new GeminiRequest.Content("model", List.of(new GeminiRequest.Part("Понял. Готов отвечать."))));

        // Добавляем историю переписки (последние 4 сообщения, чтобы не перегружать контекст)
        List<AiRequest> history = aiRequestRepository.findLastRequests(userId, PageRequest.of(0, 4));
        Collections.reverse(history);
        for (AiRequest req : history) {
            conversation.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(req.getInputText()))));
            conversation.add(new GeminiRequest.Content("model", List.of(new GeminiRequest.Part(req.getAiResponse()))));
        }

        conversation.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(userPrompt))));

        // 5. Запрос к AI
        String aiText = geminiClient.generateChat(conversation);

        // 6. Сохранение
        saveRequest(user, userPrompt, aiText);

        return aiText;
    }

    public GrantPrediction calculateGrantChance(Integer score, String major) {
        if (score == null) score = 0;

        // Если специальность не указана, ставим "любая"
        String targetMajor = (major != null && !major.isEmpty()) ? major : "Выбранная специальность";

        String prompt = String.format("""
                Роль: Опытный консультант приемной комиссии вузов Казахстана (2025).
                Задача: Оцени шанс поступления на грант (0-100%%) на основе статистики прошлых лет.
                
                Вводные данные:
                - Балл ЕНТ: %d / 140
                - Специальность: %s
                
                Инструкция для логики оценки:
                1. "Медицина", "Стоматология", "Право", "Международные отношения" — очень высокий конкурс (нужно 120+ для высокого шанса).
                2. "IT", "Инженерия", "Педагогика" — средний/высокий конкурс (нужно 95-110+).
                3. "Сельское хозяйство", "Ветеринария" — низкий конкурс (хватает 60-75).
                4. Если балл < 50 (или < 70 для медицины) — шанс 0%%.
                
                Верни ответ СТРОГО в формате JSON:
                {
                  "percentage": (целое число от 0 до 99),
                  "comment": "Короткий, но полезный комментарий именно про указанную специальность (макс 15 слов)."
                }
                """, score, targetMajor);

        try {
            List<GeminiRequest.Content> content = List.of(
                    new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(prompt)))
            );

            String jsonResponse = geminiClient.generateChat(content);

            // Очистка от возможных markdown символов
            jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(jsonResponse, GrantPrediction.class);

        } catch (Exception e) {
            log.error("Ошибка парсинга AI ответа: {}", e.getMessage());
            return new GrantPrediction(0, "Не удалось рассчитать. Попробуйте позже.");
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