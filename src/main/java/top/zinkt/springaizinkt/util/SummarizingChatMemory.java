package top.zinkt.springaizinkt.util;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通过“组合”包装一个现有 ChatMemory（通常是 MessageWindowChatMemory），
 * 在 add() 时检测是否溢出，若溢出则：
 *  1) 把旧消息拼接成原文，存入向量库（长期记忆）
 *  2) 调用模型生成摘要
 *  3) 用“摘要 + 最近 keepRecent 条原始消息”重写窗口，以控制总条数 <= maxMessages
 */
public class SummarizingChatMemory implements ChatMemory {

    private final ChatMemory delegate; // 被包装的窗口记忆
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final int maxMessages;
    private final int keepRecent; // 摘要后保留的尾部原始消息条数

    public SummarizingChatMemory(ChatMemory delegate,
                                 ChatClient chatClient,
                                 VectorStore vectorStore,
                                 int maxMessages,
                                 int keepRecent) {
        this.delegate = Objects.requireNonNull(delegate);
        this.chatClient = Objects.requireNonNull(chatClient);
        this.vectorStore = Objects.requireNonNull(vectorStore);
        this.maxMessages = maxMessages;
        this.keepRecent = Math.max(0, Math.min(keepRecent, Math.max(0, maxMessages - 1))); // 至少留出 1 条给摘要
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> all = delegate.get(conversationId);
        if (all.size() >= maxMessages) {
            // 1) 切分：旧消息（要被压缩）+ 尾部保留
            int tail = this.keepRecent;
            int split = Math.max(0, all.size() - tail);
            List<Message> toSummarize = new ArrayList<>(all.subList(0, split));
            List<Message> tailKeep = new ArrayList<>(all.subList(split, all.size()));

            String originalText = serialize(toSummarize);

            // 2) 长期记忆：把原文入库
            Map<String, Object> meta = new HashMap<>();
            meta.put("conversationId", conversationId);
            meta.put("type", "compressed-original");
            meta.put("createdAt", Instant.now().toString());
            vectorStore.add(Collections.singletonList(new Document(originalText, meta)));

            // 3) 调用模型生成摘要
            String summary = summarize(originalText);

            // 4) 用“摘要 + 尾部原始消息”重写窗口，确保 <= maxMessages
            delegate.clear(conversationId);
            delegate.add(conversationId, new SystemMessage("【会话摘要】\n" + summary));
            tailKeep.forEach(m -> delegate.add(conversationId, m));
        }
        delegate.add(conversationId, messages);

    }

    @Override
    public List<Message> get(String conversationId) {
        return delegate.get(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
    }

    private String summarize(String original) {
        // 你也可以换成系统提示词 + 工程化格式，这里给出一个稳健的中文压缩提示
        String prompt = "你是对话压缩器。将以下多轮对话压缩为 1 段简洁摘要，保留关键信息、结论、约束、未决事项、专有名词与数值。\n" +
                "不得编造新信息；若存在 TODO/后续动作，请以[TODO]列出。\n\n" +
                "对话内容：\n" + original;

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private String serialize(List<Message> messages) {
        return messages.stream()
                .map(m -> "[" + m.getMessageType() + "] " + m.getText())
                .collect(Collectors.joining("\n"));
    }
}