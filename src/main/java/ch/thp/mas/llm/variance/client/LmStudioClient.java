package ch.thp.mas.llm.variance.client;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Usage;

public class LmStudioClient implements LlmClient {

    private final com.anthropic.client.AnthropicClient client;

    public LmStudioClient(String baseUrl) {
        this.client = AnthropicOkHttpClient.builder()
                .baseUrl(baseUrl)
                .apiKey("lm-studio")
                .build();
    }

    @Override
    public LlmResponse call(String prompt, LlmRequestConfig config) throws Exception {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(config.model())
                .maxTokens(1024)
                .addUserMessage(prompt);

        if (config.temperature() != null) {
            builder.temperature(config.temperature());
        }
        if (config.topP() != null) {
            builder.topP(config.topP());
        }
        if (config.topK() != null) {
            builder.topK(config.topK().longValue());
        }

        Message message = client.messages().create(builder.build());

        for (ContentBlock block : message.content()) {
            if (block.isText()) {
                String text = block.asText().text();
                if (text != null && !text.isBlank()) {
                    return new LlmResponse(text.trim(), tokenUsage(message.usage()));
                }
            }
        }

        return new LlmResponse(message.toString(), tokenUsage(message.usage()));
    }

    private TokenUsage tokenUsage(Usage usage) {
        return TokenUsage.of(usage.inputTokens(), usage.outputTokens());
    }
}
