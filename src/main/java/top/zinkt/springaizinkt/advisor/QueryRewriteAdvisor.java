package top.zinkt.springaizinkt.advisor;

import org.springframework.core.Ordered;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * QueryRewriteAdvisor：把极短的确认/代词式用户输入（如“需要”、“好”）自动重写为
 * 带有主题的检索查询，主题由 chatMemory 的最近消息简单匹配推断（例如检索到“退票”关键词）。
 *
 * 注意：这是一个轻量启发式实现，适合多数常见场景；如果你想要更稳健的结果，
 * 可以把重写逻辑替换为“调用一个专门的短文本重写小模型（rewrite model）”。
 */

public class QueryRewriteAdvisor implements CallAdvisor, StreamAdvisor, Ordered {

    private final ChatMemory chatMemory;

    // 常见的短确认词
    private static final Set<String> CONFIRM_WORDS = new HashSet<>(Arrays.asList(
            "需要", "要", "行", "好的", "好", "可以", "是", "要的", "嗯", "行吧", "需要的", "确认"
    ));

    // 要在历史消息里匹配的主题关键字（可按需扩展）
    private static final List<String> TOPIC_KEYWORDS = Arrays.asList(
            "退票", "退票政策", "改签", "改期", "退款", "行李", "延误", "超售", "变更", "改签政策"
    );

    // 构造器
    public QueryRewriteAdvisor(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientRequest requestToUse = tryRewriteIfNeeded(chatClientRequest);
        // 传递给下一个 advisor（或最终模型）
        return callAdvisorChain.nextCall(requestToUse);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        ChatClientRequest requestToUse = tryRewriteIfNeeded(chatClientRequest);
        return streamAdvisorChain.nextStream(requestToUse);
    }

    /**
     * 判断短输入并尝试重写。若无需重写，则返回原 request；否则返回带改写后的 prompt 的新 request。
     */
    private ChatClientRequest tryRewriteIfNeeded(ChatClientRequest request) {
        Prompt prompt = request.prompt();
        if (prompt == null) {
            return request;
        }

        // 获取最后一条用户消息文本（Prompt API 提供了 getUserMessage()/getUserMessages()）
        String lastUserText = null;
        try {
            Message lastUserMsg = prompt.getUserMessage();
            if (lastUserMsg != null) {
                lastUserText = lastUserMsg.getText();
            }
        } catch (Exception ignored) {
        }

        if (lastUserText == null || lastUserText.trim().isEmpty()) {
            return request;
        }

        String normalized = lastUserText.trim();

        // 如果不是短确认/代词类，则不做改写
        if (!isShortConfirmation(normalized)) {
            return request;
        }

        // 获取会话 id（从 request.context() 里取）
        Object convIdObj = request.context() != null ? request.context().getOrDefault(ChatMemory.CONVERSATION_ID, null) : null;
        String conversationId = convIdObj != null ? convIdObj.toString() : null;
        if (conversationId == null) {
            // 如果没有会话 id，就不用重写（也可以用 DEFAULT_CONVERSATION_ID，但这通常不希望）
            return request;
        }

        // 从 ChatMemory 读取最近消息并尝试匹配话题
        List<Message> messages = chatMemory.get(conversationId);
        String detectedTopic = detectTopic(messages);

        if (detectedTopic == null) {
            // 无法从历史中推断主题 -> 不修改请求（也可以选择 fallback：把短语扩展为“请详细说明”类型的查询）
            return request;
        }

        // 生成用于向量检索的改写查询文本（尽量简洁、包含主题关键字）
        String rewriteQuery = String.format("请检索与“%s”相关的公司政策条款，并返回最相关的条目。用户回复为：\"%s\"", detectedTopic, normalized);

        // 把改写文本追加到最后一条 user message（augmentUserMessage 会返回新的 Prompt）
        Prompt newPrompt = prompt.augmentUserMessage(" " + rewriteQuery);

        // 使用 request.mutate() 构造新的 ChatClientRequest（保留 context）
        ChatClientRequest newRequest = request.mutate()
                .prompt(newPrompt)
                .build();

        return newRequest;
    }

    /** 极简的短确认判定：要么命中 CONFIRM_WORDS，要么长度非常短（3 个汉字内） */
    private boolean isShortConfirmation(String text) {
        if (text == null) return false;
        if (CONFIRM_WORDS.contains(text)) return true;
        // 如果只有一个/两个字，很有可能是确认或代词（也可能误判，但通常可接受）
        int len = text.length();
        return len <= 3;
    }

    /**
     * 从历史消息里反向查找第一个包含我们定义关键词的消息，优先 assistant 消息。
     * 返回匹配到的关键词（例如 "退票"），若未匹配返回 null。
     */
    private String detectTopic(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return null;

        // 反向遍历，优先查 assistant 消息
        for (int pass = 0; pass < 2; pass++) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message m = messages.get(i);
                if (m == null) continue;
                MessageType mt = m.getMessageType();
                // pass==0 -> 优先 assistant, pass==1 -> 任意
                if (pass == 0 && mt != MessageType.ASSISTANT) continue;
                String text = safeGetText(m);
                if (text == null) continue;
                for (String kw : TOPIC_KEYWORDS) {
                    if (text.contains(kw)) {
                        // 发现关键字，返回更友好的主题描述
                        // 比如匹配到 "退票" -> 返回 "退票政策"
                        if (kw.contains("退票")) return "退票政策";
                        if (kw.contains("改签")) return "改签/改期";
                        return kw;
                    }
                }
            }
        }
        return null;
    }

    private String safeGetText(Message m) {
        try {
            return m.getText();
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public int getOrder() {
        // 让它作为普通 advisor，具体执行顺序以 ChatClient.defaultAdvisors 中的添加顺序为主；
        // 若你想保证它比某些内置 advisor 高/低优先级，可以调整这个值。
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public String getName() {
        return "QueryRewriteAdvisor";
    }
}

