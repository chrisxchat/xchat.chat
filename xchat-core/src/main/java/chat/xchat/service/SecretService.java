package chat.xchat.service;

import chat.xchat.dto.ChatGptCredentials;
import com.google.gson.Gson;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

public class SecretService {

    private final Gson gson = new Gson();
    private SecretsManagerClient secretsManagerClient;

    private ChatGptCredentials chatGptCredentials;

    public ChatGptCredentials getChatGptCredentials() {
        if (this.chatGptCredentials == null) {
            this.chatGptCredentials = gson.fromJson(getSecret("ChatGptCredentials"), ChatGptCredentials.class);
        }
        return this.chatGptCredentials;
    }

    private String getSecret(String name) {
        return getSecretsManagerClient().getSecretValue(GetSecretValueRequest.builder()
                .secretId(name)
                .build()).secretString();
    }

    private SecretsManagerClient getSecretsManagerClient() {
        if (secretsManagerClient == null) {
            secretsManagerClient = SecretsManagerClient.builder()
                    .region(Region.of("us-east-1"))
                    .build();
        }
        return secretsManagerClient;
    }

}
