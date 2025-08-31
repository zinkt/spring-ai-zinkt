package top.zinkt.springaizinkt.config;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import top.zinkt.springaizinkt.util.ChineseTokenTextSplitter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AiConfig {

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository){
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(10)
                .build();
    }


    @Bean
    public CommandLineRunner ingestServiceDocumentToVectorStore(
            @Autowired VectorStore vectorStore,
            @Value("classpath:rag/terms-of-service.txt") Resource serviceDocument) {
        return args -> {
            // 读取并分割文本
            List<Document> documents = new ChineseTokenTextSplitter(200, 30, 5, 5000, true)
                    .transform(new TextReader(serviceDocument).read());

            // DashScope 批次大小限制
            int maxBatchSize = 10;
            List<Document> batch = new ArrayList<>();

            // 分批添加到向量存储
            for (int i = 0; i < documents.size(); i++) {
                batch.add(documents.get(i));

                // 当批次达到 maxBatchSize 或处理到最后一个文档时，添加批次
                if (batch.size() == maxBatchSize || i == documents.size() - 1) {
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
