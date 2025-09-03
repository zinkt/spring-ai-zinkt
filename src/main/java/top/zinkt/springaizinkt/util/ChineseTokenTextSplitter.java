package top.zinkt.springaizinkt.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.util.Assert;

public class ChineseTokenTextSplitter extends TextSplitter {
    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int MIN_CHUNK_SIZE_CHARS = 350;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;
    private static final int MAX_NUM_CHUNKS = 10000;
    private static final boolean KEEP_SEPARATOR = true;
    private final EncodingRegistry registry;
    private final Encoding encoding;
    private final int chunkSize;
    private final int minChunkSizeChars;
    private final int minChunkLengthToEmbed;
    private final int maxNumChunks;
    private final boolean keepSeparator;

    public ChineseTokenTextSplitter() {
        this(800, 350, 5, 10000, true);
    }

    public ChineseTokenTextSplitter(boolean keepSeparator) {
        this(800, 350, 5, 10000, keepSeparator);
    }

    public ChineseTokenTextSplitter(int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed, int maxNumChunks, boolean keepSeparator) {
        this.registry = Encodings.newLazyEncodingRegistry();
        this.encoding = this.registry.getEncoding(EncodingType.CL100K_BASE);
        this.chunkSize = chunkSize;
        this.minChunkSizeChars = minChunkSizeChars;
        this.minChunkLengthToEmbed = minChunkLengthToEmbed;
        this.maxNumChunks = maxNumChunks;
        this.keepSeparator = keepSeparator;
    }

    public static top.zinkt.springaizinkt.util.ChineseTokenTextSplitter.Builder builder() {
        return new top.zinkt.springaizinkt.util.ChineseTokenTextSplitter.Builder();
    }

    protected List<String> splitText(String text) {
        return this.doSplit(text, this.chunkSize);
    }

    protected List<String> doSplit(String text, int chunkSize) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> tokens = this.getEncodedTokens(text);
        List<String> chunks = new ArrayList<>();
        int num_chunks = 0;

        while (!tokens.isEmpty() && num_chunks < this.maxNumChunks) {
            // 先对整个剩余文本查找第一个连续两个换行符（优先级最高）
            String remainingText = this.decodeTokens(tokens);
            int firstDoubleNewline = remainingText.indexOf("\n\n");

            if (firstDoubleNewline != -1) {
                // 在第一个 "\n\n" 处切分（包含这两个换行符）
                int splitPos = firstDoubleNewline + 2;
                String chunkRaw = remainingText.substring(0, splitPos);

                String chunkTextToAppend = this.keepSeparator
                        ? chunkRaw.trim()
                        : chunkRaw.replace(System.lineSeparator(), " ").trim();

                if (chunkTextToAppend.length() > this.minChunkLengthToEmbed) {
                    chunks.add(chunkTextToAppend);
                }

                // 从 tokens 中移除已消费的部分（以 token 数为准）
                int consumed = this.getEncodedTokens(chunkRaw).size();
                tokens = tokens.subList(consumed, tokens.size());
                num_chunks++;
                continue;
            }

            // 如果剩余文本中没有连续两个换行符，则退回到基于 chunkSize 的原始逻辑
            List<Integer> window = tokens.subList(0, Math.min(chunkSize, tokens.size()));
            String windowText = this.decodeTokens(window);

            if (windowText.trim().isEmpty()) {
                tokens = tokens.subList(window.size(), tokens.size());
            } else {
                int lastPunctuation = getLastPunctuation(windowText);
                String chosen = windowText;
                if (lastPunctuation != -1 && lastPunctuation > this.minChunkSizeChars) {
                    chosen = windowText.substring(0, lastPunctuation + 1);
                }

                String chunkTextToAppend = this.keepSeparator
                        ? chosen.trim()
                        : chosen.replace(System.lineSeparator(), " ").trim();

                if (chunkTextToAppend.length() > this.minChunkLengthToEmbed) {
                    chunks.add(chunkTextToAppend);
                }

                tokens = tokens.subList(this.getEncodedTokens(chosen).size(), tokens.size());
                num_chunks++;
            }
        }

        if (!tokens.isEmpty()) {
            String remaining_text = this.decodeTokens(tokens).replace(System.lineSeparator(), " ").trim();
            if (remaining_text.length() > this.minChunkLengthToEmbed) {
                chunks.add(remaining_text);
            }
        }

        return chunks;
    }


    private static int getLastPunctuation(String chunkText) {
        int lastDoubleNewline = chunkText.lastIndexOf("\n\n");
        int lastPunctuation;
        if (lastDoubleNewline != -1) {
            // 两个换行符优先
            lastPunctuation = lastDoubleNewline;
        } else {
            lastPunctuation = Math.max(chunkText.lastIndexOf('。'), // 英文句号和中文句号
                    Math.max(
                            Math.max(chunkText.lastIndexOf('?'), chunkText.lastIndexOf('？')), // 英文问号和中文问号
                            Math.max(
                                    Math.max(chunkText.lastIndexOf('!'), chunkText.lastIndexOf('！')), // 英文感叹号和中文感叹号
                                    chunkText.lastIndexOf('\n') // 单个换行符
                            )
                    )
            );
        }
        return lastPunctuation;
    }

    private List<Integer> getEncodedTokens(String text) {
        Assert.notNull(text, "Text must not be null");
        return this.encoding.encode(text).boxed();
    }

    private String decodeTokens(List<Integer> tokens) {
        Assert.notNull(tokens, "Tokens must not be null");
        IntArrayList tokensIntArray = new IntArrayList(tokens.size());
        Objects.requireNonNull(tokensIntArray);
        tokens.forEach(tokensIntArray::add);
        return this.encoding.decode(tokensIntArray);
    }

    public static final class Builder {
        private int chunkSize = 800;
        private int minChunkSizeChars = 350;
        private int minChunkLengthToEmbed = 5;
        private int maxNumChunks = 10000;
        private boolean keepSeparator = true;

        private Builder() {
        }

        public top.zinkt.springaizinkt.util.ChineseTokenTextSplitter.Builder withChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public top.zinkt.springaizinkt.util.ChineseTokenTextSplitter.Builder withMinChunkSizeChars(int minChunkSizeChars) {
            this.minChunkSizeChars = minChunkSizeChars;
            return this;
        }

        public top.zinkt.springaizinkt.util.ChineseTokenTextSplitter.Builder withMinChunkLengthToEmbed(int minChunkLengthToEmbed) {
            this.minChunkLengthToEmbed = minChunkLengthToEmbed;
            return this;
        }

        public top.zinkt.springaizinkt.util.ChineseTokenTextSplitter.Builder withMaxNumChunks(int maxNumChunks) {
            this.maxNumChunks = maxNumChunks;
            return this;
        }

        public top.zinkt.springaizinkt.util.ChineseTokenTextSplitter.Builder withKeepSeparator(boolean keepSeparator) {
            this.keepSeparator = keepSeparator;
            return this;
        }

        public top.zinkt.springaizinkt.util.ChineseTokenTextSplitter build() {
            return new top.zinkt.springaizinkt.util.ChineseTokenTextSplitter(this.chunkSize, this.minChunkSizeChars, this.minChunkLengthToEmbed, this.maxNumChunks, this.keepSeparator);
        }
    }
}
