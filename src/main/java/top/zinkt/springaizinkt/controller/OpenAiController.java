package top.zinkt.springaizinkt.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import top.zinkt.springaizinkt.services.LightweightService;
import top.zinkt.springaizinkt.services.TaskRouterService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static top.zinkt.springaizinkt.services.TaskRouterService.TaskType.COMPLEX_GENERAL_PURPOSE;

@RestController
@CrossOrigin
public class OpenAiController  {
    @Autowired @Qualifier("chatClientDefault")
    private ChatClient chatClientDefault;
    @Autowired @Qualifier("vectorStoreMemory")
    private VectorStore vectorStoreMemory;
    @Autowired
    private TaskRouterService taskRouterService;
    @Autowired
    private LightweightService lightweightService;

    //    todo: 根据用户询问，判断用户购票欲望，调整机票价格
    @CrossOrigin
    @GetMapping(value = "/ai/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStreamAsString(@RequestParam(value = "message", defaultValue = "讲个笑话") String message,
                                               @RequestParam(value = "conversationId", defaultValue = "10") String conversationId) {
        // 路由
        TaskRouterService.TaskType taskType = COMPLEX_GENERAL_PURPOSE;
//        taskType = taskRouterService.determineTaskType(message);
        Flux<String> responseFlux = null;

        switch (taskType) {
            case LIGHTWEIGHT_QA:
                responseFlux = lightweightService.handle(message);
                break;
            case TOOL_CALL:
            case RAG_QUERY:
            case COMPLEX_GENERAL_PURPOSE:
                // 1) 检索长期记忆
                List<Document> docs = vectorStoreMemory.similaritySearch(
                        SearchRequest.builder()
                                .query(message)
                                .topK(3)
                                .filterExpression(new FilterExpressionBuilder().eq("conversationId", conversationId).build())
                                .build()
                );

                String longTermContext = docs.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n---\n"));

                String augmentedUser = new StringBuilder()
                        .append("【长期参考（仅供参考，可忽略）】\n")
                        .append(longTermContext.isBlank() ? "(无长期上下文)" : longTermContext)
                        .append("\n\n")
                        .append("【用户最新请求 — 仅列业务关键字段，模型应从中提取并在收齐字段后调用相应 tool】\n")
                        .append(message)
                        .toString();

                responseFlux = chatClientDefault.prompt()
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .user(augmentedUser)
                        .stream()
                        .content();

                break;
            default:
                break;
        }

        return responseFlux.concatWith(Flux.just("[complete]"));
    }

}

