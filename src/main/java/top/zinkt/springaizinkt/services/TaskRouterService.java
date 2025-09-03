package top.zinkt.springaizinkt.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class TaskRouterService {
    @Autowired
    @Qualifier("chatClientTaskRouter")
    private ChatClient chatClient;

    public TaskType determineTaskType(String message) {
        String response = chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .param("user_message", message)
                )
                .call().content();

        // 根据LLM的回复判断任务类型
        if (response.contains("LIGHTWEIGHT_QA")) {
            return TaskType.LIGHTWEIGHT_QA;
        } else if (response.contains("TOOL_CALL")) {
            return TaskType.TOOL_CALL;
        } else if (response.contains("RAG_QUERY")) {
            return TaskType.RAG_QUERY;
        } else {
            return TaskType.COMPLEX_GENERAL_PURPOSE; // 默认或者复杂任务
        }
    }

    public enum TaskType {
        LIGHTWEIGHT_QA,               // 轻量级问答，例如简单的寒暄、常识
        TOOL_CALL,                    // 需要调用工具的任务，例如查询/取消预订
        RAG_QUERY,                    // 需要从知识库检索的任务
        COMPLEX_GENERAL_PURPOSE       // 复杂的通用任务，可能需要更长的思考时间或多轮交互
    }
}
