package ch.thp.cas.llm.variance.client;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;

public class OpenAiClient implements LlmClient {

    private final OpenAIClient client;

    public OpenAiClient(String apiKey) {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public String call(String prompt, LlmRequestConfig config) throws Exception {
        ResponseCreateParams.Builder builder = ResponseCreateParams.builder()
                .model(config.model())
                .input(prompt);

        if (config.temperature() != null) {
            builder.temperature(config.temperature());
        }
        if (config.topP() != null) {
            builder.topP(config.topP());
        }

        Response response = client.responses().create(builder.build());

        for (ResponseOutputItem item : response.output()) {
            if (item.isMessage()) {
                ResponseOutputMessage msg = item.asMessage();
                for (ResponseOutputMessage.Content content : msg.content()) {
                    if (content.isOutputText()) {
                        String text = content.asOutputText().text();
                        if (text != null && !text.isBlank()) {
                            return text.trim();
                        }
                    }
                }
            }
        }

        return response.toString();
    }
}
