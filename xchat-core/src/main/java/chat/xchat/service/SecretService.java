package chat.xchat.service;

import chat.xchat.dto.ChatGptCredentials;
import chat.xchat.dto.secrets.DatabaseSecret;
import com.google.gson.Gson;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

public class SecretService {

    private static final String CHAT_GPT_CREDENTIALS = "ChatGptCredentials";
    private static final String DATABASE_SECRET = "DatabaseSecret";
    private static final String DATABASE_PROXY_HOST = "DATABASE_PROXY_HOST";
    private static final String DATABASE_PORT = "DATABASE_PORT";
    private static final String DATABASE_PASSWORD = "DATABASE_PASSWORD";
    private static final String DATABASE_USERNAME = "DATABASE_USERNAME";
    private static final String DATABASE_NAME = "DATABASE_NAME";
    private static final String CHAT_GPT_API_KEY = "CHAT_GPT_API_KEY";
    private static final String CHAT_GPT_ORG_ID = "CHAT_GPT_ORG_ID";

    private final Gson gson = new Gson();
    private SecretsManagerClient secretsManagerClient;

    private ChatGptCredentials chatGptCredentials;
    private DatabaseSecret databaseSecret;

    public ChatGptCredentials getChatGptCredentials() {
        if (this.chatGptCredentials == null) {
            if (System.getenv(CHAT_GPT_API_KEY) == null) {
                this.chatGptCredentials = gson.fromJson(getSecret(CHAT_GPT_CREDENTIALS), ChatGptCredentials.class);
            } else {
                this.chatGptCredentials = getChatGptCredentialsFromEnv();
            }
        }
        return this.chatGptCredentials;
    }

    public DatabaseSecret getDatabaseSecret() {
        if (this.databaseSecret == null) {
            if (System.getenv(DATABASE_PROXY_HOST) == null) {
                this.databaseSecret = gson.fromJson(getSecret(DATABASE_SECRET), DatabaseSecret.class);
            } else {
                this.databaseSecret = getDatabaseSecretFromEnv();
            }
        }
        return this.databaseSecret;
    }

    private String getSecret(String name) {
        return getSecretsManagerClient().getSecretValue(GetSecretValueRequest.builder()
                .secretId(name)
                .build()).secretString();
    }

    private SecretsManagerClient getSecretsManagerClient() {
        if (this.secretsManagerClient == null) {
            this.secretsManagerClient = SecretsManagerClient.builder()
                    .region(Region.of("us-east-1"))
                    .build();
        }
        return this.secretsManagerClient;
    }

    private DatabaseSecret getDatabaseSecretFromEnv() {
        DatabaseSecret result = new DatabaseSecret();
        result.setDatabase(System.getenv(DATABASE_NAME));
        result.setUsername(System.getenv(DATABASE_USERNAME));
        result.setPassword(System.getenv(DATABASE_PASSWORD));
        result.setPort(System.getenv(DATABASE_PORT));
        result.setProxyHost(System.getenv(DATABASE_PROXY_HOST));
        return result;
    }

    private ChatGptCredentials getChatGptCredentialsFromEnv() {
        ChatGptCredentials result = new ChatGptCredentials();
        result.setApiKey(System.getenv(CHAT_GPT_API_KEY));
        result.setOrgId(System.getenv(CHAT_GPT_ORG_ID));
        return result;
    }

}
