package itfest.backend.dto;

import lombok.Data;

@Data
public class AiChatRequest {
    private Long userId;
    private String prompt;
}
