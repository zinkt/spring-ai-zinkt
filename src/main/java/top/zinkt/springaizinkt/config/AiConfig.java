package top.zinkt.springaizinkt.config;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class AiConfig {
    @Bean
    public ChatMemoryRepository chatMemoryRepository(){
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    public VectorStore vectorStore(DashScopeEmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public CommandLineRunner ingestServiceDocumentToVectorStore(DashScopeEmbeddingModel embeddingModel, VectorStore vectorStore
    , @Value("classpath:rag/terms-of-service.txt") Resource serviceDocument) {
        return args -> {
            vectorStore.write(
                    new TokenTextSplitter().transform(
                            new TextReader(serviceDocument).read()
                    )
            );
        };
    }

}
