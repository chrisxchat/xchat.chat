package chat.xchat.service;

import chat.xchat.dto.LoopMessageKeys;
import chat.xchat.dto.LoopMessageRequest;
import chat.xchat.dto.UnansweredQuestion;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class IMessageService implements CommunicationService {

    private static final String LOOP_MESSAGE_API = "https://server.loopmessage.com/api/v1/message/send/";
    private static final Integer MESSAGE_MAX_SYMBOLS = 10_000;

    private final SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
            .region(Region.of("us-east-1"))
            .build();

    private final Gson gson = new Gson();

    private final HttpClient client = HttpClient.newHttpClient();

    private final LoopMessageKeys loopMessageKeys = gson.fromJson(getSecret("LoopMessageKeys"), LoopMessageKeys.class);

    private LambdaLogger logger;

    public IMessageService(LambdaLogger logger) {
        this.logger = logger;
    }

    public void sendUnansweredQuestion(UnansweredQuestion question, String answer) {
        LoopMessageRequest request = new LoopMessageRequest();
        request.setText(answer);
        request.setGroup(question.getChatId());
        request.setRecipient(question.getUser());
        request.setSender_name(question.getSenderName());
        sendLoopMessage(request);
    }

    public int sendLoopMessage(LoopMessageRequest request) {
        String bodyStr = this.gson.toJson(request);
        return sendLoopMessage(bodyStr);
    }

    public int sendLoopMessage(String bodyStr) {
        HttpRequest loopMessageRequest = HttpRequest.newBuilder()
                .uri(URI.create(LOOP_MESSAGE_API))
                .header("Content-Type", "application/json")
                .header("Authorization", this.loopMessageKeys.getAuthorizationKey())
                .header("Loop-Secret-Key", this.loopMessageKeys.getSecretAPIKey())
                .POST(HttpRequest.BodyPublishers.ofString(bodyStr))
                .build();
        try {
            HttpResponse<String> res = client.send(loopMessageRequest, HttpResponse.BodyHandlers.ofString());
            this.logger.log("STATUS CODE: " + res.statusCode());
            return res.statusCode();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean validate() {
        if (this.loopMessageKeys.getAuthorizationKey() == null || this.loopMessageKeys.getAuthorizationKey().isEmpty()) {
            this.logger.log("[ERROR] Empty Loop Authorization Key");
            return false;
        }
        if (this.loopMessageKeys.getSecretAPIKey() == null || this.loopMessageKeys.getSecretAPIKey().isEmpty()) {
            this.logger.log("[ERROR] Empty Loop Secret Key");
            return false;
        }
        return true;
    }

    private String getSecret(String name) {
        return secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
                .secretId(name)
                .build()).secretString();
    }

}
