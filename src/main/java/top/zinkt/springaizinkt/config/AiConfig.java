package top.zinkt.springaizinkt.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import top.zinkt.springaizinkt.advisor.QueryRewriteAdvisor;
import top.zinkt.springaizinkt.util.ChineseTokenTextSplitter;
import top.zinkt.springaizinkt.util.SummarizingChatMemory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class AiConfig {
    @Bean
    public VectorStore vectorStoreRAG(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName("vector_store")   // 表1
                .initializeSchema(false)
                .build();
    }

    @Bean
    public VectorStore vectorStoreMemory(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName("vector_store_memory") // 表2
                .initializeSchema(false)
                .build();
    }

    @Bean
    public ChatClient chatClientDefault(ChatClient.Builder chatClientBuilder, @Qualifier("vectorStoreRAG") VectorStore vectorStore,
                                        @Value("classpath:/templates/default-prompt.st") Resource promptResource,
                                        @Qualifier("chatMemory") ChatMemory chatMemory,
                                        @Autowired ToolCallbackProvider toolCallbackProvider) {

        return chatClientBuilder
                .defaultSystem(promptResource)
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder().build(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new QueryRewriteAdvisor(chatMemory),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .similarityThreshold(0.6d)
                                        .topK(6)
                                        .build())
                                .build()
                )
                .defaultToolNames(
                        "cancelBookingRequestTool",
                        "getBookingDetailsTools",
                        "changeBookingRequestTool",
                        "bookFlightRequestTool",
                        "getFlightPriceRequestTool"
                )
//                .defaultToolCallbacks(toolCallbackProvider) // 保留你的回调提供者
                .build();

    }

    @Bean
    public ChatClient chatClientTaskRouter(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder
                .defaultSystem("你是一个任务路由器，你的目标是根据用户输入判断任务的类型。请从以下选项中选择一个最匹配的类型，并只返回类型名称：\n" +
                        "\n" +
                        "选项：\n" +
                        "- LIGHTWEIGHT_QA: 如果用户的问题非常简单，可以直接回答，或者是一个简单的寒暄、问候。\n" +
                        "- TOOL_CALL: 如果用户的问题明显需要调用某个工具（例如预订查询、取消预订）。\n" +
                        "- RAG_QUERY: 如果用户的问题需要从提供的知识库中检索信息来回答（例如关于服务条款的问题）。\n" +
                        "- COMPLEX_GENERAL_PURPOSE: 如果用户的问题不属于上述任何一种，或者是一个需要更深入理解和推理的复杂问题。\n" +
                        "\n" +
                        "用户输入: ${user_message}\n")
                .build();
    }

    @Bean
    public ChatClient chatClientLightweight(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder
                .defaultSystem("你是一个友好的AI助手，擅长快速回答简单问题和进行日常对话。")
                .defaultOptions(ChatOptions.builder()
                        .model("qwen-max")
                        .temperature(0.7)
                        .build())
                .build();
    }

    @Bean
    public ChatClient chatClientPromptCompress(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder
                .defaultOptions(ChatOptions.builder()
                        .model("qwen-plus")
                        .temperature(0.6)
                        .build())
                .build();
    }

    @Bean
    public ChatMemory baseWindowMemory(ChatMemoryRepository repo) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo)
                .maxMessages(10) // 窗口大小
                .build();
    }

    @Bean
    public ChatMemory chatMemory(@Qualifier("chatClientPromptCompress") ChatClient chatClientPromptCompress,
                                 @Qualifier("vectorStoreMemory") VectorStore vectorStoreMemory,
                                 @Qualifier("baseWindowMemory") ChatMemory baseWindowMemory) {
        // keepRecent = 4 表示摘要后仍保留最近 4 条原始消息
        return new SummarizingChatMemory(baseWindowMemory, chatClientPromptCompress, vectorStoreMemory, 10, 4);
    }

    @Bean
    @Profile("init-vectorstore")
    public CommandLineRunner ingestServiceDocument(
            @Qualifier("vectorStoreRAG") VectorStore vectorStore,
            @Value("classpath:rag/terms-of-service-short.txt") Resource serviceDocument,
            @Value("classpath:templates/ingest-service-prompt.st") Resource ingestServiceDocument,
            @Qualifier("chatClientPromptCompress") ChatClient chatClient) {
        return args -> {

            // 1. 读取文本
            String content = new TextReader(serviceDocument).read().stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));

            String ingestServicePrompt = content;
//          ingestServicePrompt = ingestServiceDocument.getContentAsString(StandardCharsets.UTF_8);

            String summarizedContent = chatClient.prompt()
                    .system(ingestServicePrompt)
                    .user(content)
                    .call().content();
//            String summarizedContent = """
//
//                    """;

            // 2. 文本分块
            List<Document> chunks = new ChineseTokenTextSplitter(150, 5, 5, 5000, true)
                    .transform(List.of(new Document(summarizedContent)));

            // DashScope 批次大小限制
            int maxBatchSize = 10;
            List<Document> batch = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                batch.add(chunks.get(i));

                // 当批次达到 maxBatchSize 或处理到最后一个文档时，添加批次
                if (batch.size() == maxBatchSize || i == chunks.size() - 1) {
                    try {
                        vectorStore.add(new ArrayList<>(batch));
                        batch.clear(); // 清空批次
                    } catch (Exception e) {
                        throw new RuntimeException("向量存储添加失败", e);
                    }
                }
            }


        };
    }


}
