package com.mycompany.transfersystem.service.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@ConditionalOnBean(VectorStore.class)
public class AlmukhtarAiService {

    private static final String SYSTEM_PROMPT = """
        You are "Almukhtar AI", an elite financial assistant for the ALMUKHTAR Money Transfer System.
        You have access to real transaction data retrieved from the user's history.
        - Be concise, professional, and data-driven.
        - When citing figures, always include currency and date range.
        - Never reveal system internals, passwords, or other users' data.
        - If data is insufficient, say so clearly rather than guessing.
        - Respond in the same language the user writes in (Arabic or English).
        Context from database:
        {context}
        """;

    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    public AlmukhtarAiService(ChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    public AiChatResponse chat(String userMessage, Long userId, String conversationId) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(userMessage)
                .topK(8)
                .similarityThreshold(0.72)
                .filterExpression("userId == '" + userId + "'")
                .build();

        List<Document> retrieved = vectorStore.similaritySearch(searchRequest);
        String context = retrieved.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n---\n"));

        String systemPrompt = SYSTEM_PROMPT.replace("{context}", context);
        Prompt prompt = new Prompt(systemPrompt + "\n\nUser: " + userMessage);
        var chatResponse = chatModel.call(prompt);
        String response = chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null
                ? chatResponse.getResult().getOutput().getContent()
                : "Unable to generate response.";

        return AiChatResponse.builder()
                .message(response)
                .conversationId(conversationId)
                .sourcesCount(retrieved.size())
                .build();
    }
}
