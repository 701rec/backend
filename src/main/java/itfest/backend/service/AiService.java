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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiRequestRepository aiRequestRepository;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final GeminiClient geminiClient;


    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Роль: Ты — профессиональный консультант по образованию.
            
            Данные пользователя:
            - Имя: %s
            - Балл ЕНТ: %s
            - Город: %s
            
            Инструкции: 
            1. Если пользователь спрашивает "куда поступить", опирайся на его балл ЕНТ. 
            2. Если пользователь не указал город, предлагай варианты в его родном городе (%s). 
            3. ИСПОЛЬЗУЙ предоставленную ниже информацию об университетах для ответа. Если информации нет, отвечай общими знаниями.
            4. Не придумывай цены и телефоны, если их нет в предоставленном списке.
            
            Информация из нашей базы данных (учитывай её в первую очередь!):
            %s
            
            Твоя задача — вести связный диалог. Помни предыдущие вопросы.
            """;

    public String getAnswer(Long userId, String userPrompt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        List<University> foundUniversities = universityRepository.search(userPrompt);

        if (foundUniversities.isEmpty() && userPrompt.toLowerCase().contains("универ")) {
            foundUniversities = universityRepository.findAll().stream().limit(3).toList();
        }

        String dbContext = formatUniversityData(foundUniversities);

        String entScore = (user.getEntScore() != null) ? String.valueOf(user.getEntScore()) : "Не указано";
        String location = (user.getLocation() != null) ? user.getLocation() : "Не указан";
        String name = (user.getFirstName() != null) ? user.getFirstName() : "Абитуриент";

        String systemInstruction = String.format(SYSTEM_PROMPT_TEMPLATE,
                name, entScore, location, location, dbContext);

        List<GeminiRequest.Content> conversation = new ArrayList<>();
        conversation.add(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(systemInstruction))));
        conversation.add(new GeminiRequest.Content("model", List.of(new GeminiRequest.Part("Понял, данные базы принял. Готов отвечать."))));

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
                .map(u -> String.format("- %s (%s): Цена: %s, Рейтинг: %s, Гранты: %s, Адрес: %s",
                        u.getName(), u.getShortName(), u.getPrice(), u.getRating(),
                        (u.getMilitary() ? "Есть военка" : "Нет военки"), u.getLocation()))
                .collect(Collectors.joining("\n"));
    }
}