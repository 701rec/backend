package itfest.backend.service;

import itfest.backend.client.GeminiClient;
import itfest.backend.dto.gemini.GeminiRequest;
import itfest.backend.exception.ResourceNotFoundException;
import itfest.backend.model.AiRequest;
import itfest.backend.model.University;
import itfest.backend.model.User;
import itfest.backend.repository.AiRequestRepository;
import itfest.backend.repository.UniversityRepository;
import itfest.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiRequestRepository aiRequestRepository;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final GeminiClient geminiClient;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Роль: Ты — консультант по поступлению (IT Fest).
            
            Данные пользователя:
            - Имя: %s
            - Балл ЕНТ: %s
            - Город: %s
            
            Твои правила:
            1. Используй данные из списка ниже для ответов на вопросы о ценах, специальностях и общежитиях.
            2. Если данных нет в списке, отвечай, что "в моей базе пока нет информации об этом вузе", и предлагай посмотреть другие.
            3. Веди себя естественно.
            
            НАЙДЕННЫЕ УНИВЕРСИТЕТЫ (Релевантные запросу):
            %s
            """;

    public String getAnswer(Long userId, String userPrompt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        List<University> relevantUniversities;

        if (isGreeting(userPrompt)) {
            relevantUniversities = new ArrayList<>();
        } else {
            String[] words = userPrompt.replaceAll("[^a-zA-Zа-яА-Я0-9 ]", "").split("\\s+");
            Set<University> foundSet = new HashSet<>();

            for (String word : words) {
                if (word.length() >= 2) {
                    foundSet.addAll(universityRepository.searchByKeyword(word));
                }
            }

            relevantUniversities = new ArrayList<>(foundSet);

            if (relevantUniversities.isEmpty() && (userPrompt.toLowerCase().contains("поступ") || userPrompt.toLowerCase().contains("универ"))) {
                relevantUniversities = universityRepository.findTopRated(PageRequest.of(0, 3));
            }
        }

        String dbContext = formatUniversityData(relevantUniversities);

        String entScore = (user.getEntScore() != null) ? String.valueOf(user.getEntScore()) : "Не указано";
        String location = (user.getLocation() != null) ? user.getLocation() : "Не указан";
        String name = (user.getFirstName() != null) ? user.getFirstName() : "Друг";

        String systemInstruction = String.format(SYSTEM_PROMPT_TEMPLATE,
                name, entScore, location, dbContext);

        List<GeminiRequest.Content> conversation = new ArrayList<>();
        conversation.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(systemInstruction))));
        conversation.add(new GeminiRequest.Content("model", List.of(new GeminiRequest.Part("Принято."))));

        List<AiRequest> history = aiRequestRepository.findLastRequests(userId, PageRequest.of(0, 6));
        Collections.reverse(history);

        for (AiRequest req : history) {
            conversation.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(req.getInputText()))));
            conversation.add(new GeminiRequest.Content("model", List.of(new GeminiRequest.Part(req.getAiResponse()))));
        }

        conversation.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(userPrompt))));

        String aiText = geminiClient.generateChat(conversation);

        AiRequest newRecord = AiRequest.builder()
                .user(user)
                .inputText(userPrompt)
                .aiResponse(aiText)
                .build();
        aiRequestRepository.save(newRecord);

        return aiText;
    }

    private String formatUniversityData(List<University> universities) {
        if (universities == null || universities.isEmpty()) {
            return "Нет конкретной информации в базе по этому запросу.";
        }
        return universities.stream()
                .limit(5)
                .map(u -> String.format("[%s (%s): %s тг, Рейтинг %s, Профиль: %s]",
                        u.getName(), u.getShortName(), u.getPrice(), u.getRating(), u.getFocus()))
                .collect(Collectors.joining("\n"));
    }

    private boolean isGreeting(String text) {
        if (text == null) return false;
        String s = text.toLowerCase().trim();
        return s.equals("привет") || s.equals("салам") || s.equals("hello") || s.equals("hi") || s.equals("здравствуйте");
    }
}