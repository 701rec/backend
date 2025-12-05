package itfest.backend.service;

import itfest.backend.model.University;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class UniversitySpecification {

    public static Specification<University> searchByKeywords(List<String> keywords) {
        return (root, query, builder) -> {
            if (keywords == null || keywords.isEmpty()) {
                // Если ключевых слов нет — возвращаем условие "всегда истина" (показать всё)
                return builder.conjunction();
            }

            Specification<University> finalSpec = null;

            for (String word : keywords) {
                String pattern = "%" + word.toLowerCase() + "%";

                // Создаем спецификацию для текущего слова
                Specification<University> wordSpec = (r, q, b) -> b.or(
                        b.like(b.lower(r.get("name")), pattern),
                        b.like(b.lower(r.get("shortName")), pattern),
                        b.like(b.lower(r.get("focus")), pattern),
                        b.like(b.lower(r.get("description")), pattern)
                );

                // Объединяем спецификации
                if (finalSpec == null) {
                    finalSpec = wordSpec; // Если это первое слово, оно становится основой
                } else {
                    finalSpec = finalSpec.or(wordSpec); // Иначе добавляем через OR
                }
            }

            // Если по какой-то причине finalSpec остался null (хотя проверка выше это исключает), вернем всё
            if (finalSpec == null) {
                return builder.conjunction();
            }

            return finalSpec.toPredicate(root, query, builder);
        };
    }
}