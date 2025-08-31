package top.zinkt.springaizinkt.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

public class RereadingAdvisor implements BaseAdvisor {
    private static  String DEFAULT_USER_TEXT_ADVISE = """
            {original_prompt}
            Read the question again: {original_prompt}
            """;
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
//        请求之前，重写提示词
        String contents = chatClientRequest.prompt().getContents();
        String rereadQuery = PromptTemplate.builder().template(DEFAULT_USER_TEXT_ADVISE).build()
                .render(Map.of("original_prompt",contents));

        return chatClientRequest.mutate().
                prompt(Prompt.builder().content(rereadQuery).build())
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
