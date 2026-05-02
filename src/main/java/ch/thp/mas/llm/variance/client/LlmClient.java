package ch.thp.mas.llm.variance.client;

public interface LlmClient {
    String call(String prompt, LlmRequestConfig config) throws Exception;
}
