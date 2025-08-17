package top.zinkt.springaizinkt.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@RestController
@CrossOrigin
public class OpenAiController  {

    @Autowired
    private ChatMemoryRepository chatMemoryRepository;
    ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(10)
            .build();

    private final ChatClient chatClient;

    public OpenAiController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        您是"乐山航空"公司的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复，您正在通过在线聊天系统与客户互动。
                        在更改或退订之前，您必须始终从客户处获取一下信息：预定号、客户姓名，并且在用户确认之后，再执行更改或退订。
                        在询问客户之前，请检查消息历史记录以获取此信息。
                        请讲中文。
                        今天的日期是 {current_date}.""")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).searchRequest(SearchRequest.builder().similarityThreshold(0.6d).topK(6).build())
                                .build()
                )
                .defaultToolNames("cancelBookingRequestTool","getBookingDetails")
                .build();
    }

    @CrossOrigin
    @GetMapping(value = "/ai/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStreamAsString(@RequestParam(value = "message", defaultValue = "讲个笑话") String message) {
        Flux<String> content = this.chatClient.prompt()
                .advisors()
                .user(message)
                .system(promptSystemSpec -> promptSystemSpec.param("current_date", LocalDate.now().toString()))
                .stream()
                .content();
        return content.concatWith(Flux.just("[complete]"));
    }



}
