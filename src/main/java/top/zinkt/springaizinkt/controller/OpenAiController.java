package top.zinkt.springaizinkt.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import top.zinkt.springaizinkt.advisor.RereadingAdvisor;

import java.time.LocalDate;

@RestController
@CrossOrigin
public class OpenAiController  {
    private final ChatClient chatClient;

    public OpenAiController(ChatClient.Builder chatClientBuilder,@Autowired VectorStore vectorStore,
                            @Value("classpath:/templates/prompt.st") Resource promptResource,
                            @Autowired ChatMemory chatMemory,
                            @Autowired ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chatClientBuilder
                .defaultSystem(promptResource)
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder().build(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).searchRequest(SearchRequest.builder().similarityThreshold(0.6d).topK(6).build())
                                .build()
                )
                .defaultToolNames("cancelBookingRequestTool","getBookingDetails")
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

//    todo: 根据用户询问，判断用户购票欲望，调整机票价格
    @CrossOrigin
    @GetMapping(value = "/ai/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStreamAsString(@RequestParam(value = "message", defaultValue = "讲个笑话") String message) {
        Flux<String> content = this.chatClient.prompt()
                .user(message)
//                .advisors(new RereadingAdvisor())
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, "3"))
                .system(promptSystemSpec -> promptSystemSpec
                        .param("current_date", LocalDate.now().toString())
                        .param("name", "徐庶")
                        .param("age", 26)
                )
                .stream()
                .content();
        return content.concatWith(Flux.just("[complete]"));
    }



}
