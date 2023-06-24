package chat.xchat.service;

import chat.xchat.dto.ChatGptRequest;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChatGptService {

    private LambdaLogger logger;

    private static final Gson gson = new Gson();

    private static final HttpClient client = HttpClient.newHttpClient();

    private final SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
            .region(Region.of("us-east-1"))
            .build();

    private final String chatGptApiKey = getSecret("ChatGptApiKey");

    public ChatGptService(LambdaLogger logger) {
        this.logger = logger;
    }

    private String getSecret(String name) {
        return secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
                .secretId(name)
                .build()).secretString();
    }

    public String askChatGpt(String message) throws IOException, InterruptedException {
        if (!validateCredentials()) {
            throw new RuntimeException("ChatGptService: Credentials validation failed");
        }
        String bodyStr = gson.toJson(new ChatGptRequest(message));
         logger.log("[ChatGPT] Asking: " + bodyStr);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + this.chatGptApiKey)
                .header("OpenAI-Organization", System.getenv("OPENAI_ORG_ID"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyStr))
                .build();
        HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
        return obj.getAsJsonArray("choices").get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString();
    }

    private boolean validateCredentials() {
        if (this.chatGptApiKey == null || this.chatGptApiKey.isBlank()) {
            this.logger.log("[ERROR] Can not get ChatGPT API key");
            return false;
        }
        String openaiOrgId = System.getenv("OPENAI_ORG_ID");
        if (openaiOrgId == null || openaiOrgId.isBlank()) {
            this.logger.log("[ERROR] Can not get OPENAI_ORG_ID");
            return false;
        }
        return true;
    }

}
