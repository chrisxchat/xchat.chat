package chat.xchat.service;

import chat.xchat.dto.ChatGptRequest;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChatGptService {

    private LambdaLogger logger;

    private Gson gson;

    private HttpClient client;

    private SecretService secretService;

    public ChatGptService(LambdaLogger logger) {
        this.logger = logger;
        this.secretService = new SecretService();
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public String askChatGpt(String message, Integer responseLimit) {
        validateCredentials();
        String bodyStr = gson.toJson(new ChatGptRequest(message));
        logger.log("[ChatGPT] Asking: " + bodyStr);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + secretService.getChatGptCredentials().getApiKey())
                .header("OpenAI-Organization", secretService.getChatGptCredentials().getOrgId())
                .POST(HttpRequest.BodyPublishers.ofString(bodyStr))
                .build();
        try {
            HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
            String response = obj.getAsJsonArray("choices").get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString();
            if (responseLimit == null) {
                return response;
            }
            return response.length() > responseLimit ? response.substring(0, responseLimit) : response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void validateCredentials() {
        this.logger.log("Validating chat gpt credentials");
        try {
            String apiKey = secretService.getChatGptCredentials().getApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                this.logger.log("[ERROR] Can not get ChatGPT API key");
                throw new RuntimeException("[ERROR] Can not get ChatGPT API key");
            }
            String openaiOrgId = secretService.getChatGptCredentials().getOrgId();
            if (openaiOrgId == null || openaiOrgId.isBlank()) {
                this.logger.log("[ERROR] Can not get OPENAI_ORG_ID");
                throw new RuntimeException("[ERROR] Can not get OPENAI_ORG_ID");
            }
        } catch (Throwable t) {
            this.logger.log("Exception during chat gpt credentials validation " + t.getMessage());
        }
        this.logger.log("Chat gpt credentials validated");
    }

}
