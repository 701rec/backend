package itfest.backend.dto.gemini;

import java.util.List;

public record GeminiRequest(List<Content> contents) {
    public record Content(String role, List<Part> parts) {}
    public record Part(String text) {}
}