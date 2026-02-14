package ch.thp.cas.llm.variance.client;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

public class AnthropicClient implements LlmClient {

    private final com.anthropic.client.AnthropicClient client;

    public AnthropicClient(String apiKey) {
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public String call(String prompt, LlmRequestConfig config) throws Exception {
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
                    return text.trim();
                }
            }
        }

        return message.toString();
    }
}
