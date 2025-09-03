package top.zinkt.springaizinkt.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class LightweightService {
    @Qualifier("chatClientTaskRouter")
    private ChatClient chatClient;

    public Flux<String> handle(String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }
}
