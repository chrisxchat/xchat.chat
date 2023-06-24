package chat.xchat;

import chat.xchat.dto.LoopMessageDto;
import chat.xchat.dto.LoopMessageKeys;
import chat.xchat.dto.LoopMessageRequest;
import chat.xchat.service.ChatGptService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoopMessageIMessagesFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private LambdaLogger logger;
    private final Gson gson = new Gson();

    private final HttpClient client = HttpClient.newHttpClient();

    private static final String LOOP_MESSAGE_API = "https://server.loopmessage.com/api/v1/message/send/";

    private final SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
            .region(Region.of("us-east-1"))
            .build();

    private final LoopMessageKeys loopMessageKeys = gson.fromJson(getSecret("LoopMessageKeys"), LoopMessageKeys.class);

    private ChatGptService chatGptService;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        this.logger = context.getLogger();
        this.chatGptService = new ChatGptService(this.logger);
        this.logger.log("REQUEST: " + requestEvent.getBody());
        if (!validate()) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500);
        }
        try {
            LoopMessageDto incomingRequest = this.gson.fromJson(requestEvent.getBody(), LoopMessageDto.class);
            if (!"message_inbound".equals(incomingRequest.getAlert_type())) {
                this.logger.log("Alert type not supported");
                return new APIGatewayProxyResponseEvent().withStatusCode(200);
            }
            String phoneNumber = incomingRequest.getRecipient();
            if (phoneNumber.startsWith("\\+")) {
                phoneNumber = phoneNumber.replaceAll("\\+", "");
            }
            String chatGptResponse = this.chatGptService.askChatGpt(incomingRequest.getText());
            String bodyStr = this.gson.toJson(new LoopMessageRequest(phoneNumber, chatGptResponse, System.getenv("SENDER_NAME")));
            this.logger.log("RESPONSE message: " + bodyStr);
            HttpRequest loopMessageRequest = HttpRequest.newBuilder()
                    .uri(URI.create(LOOP_MESSAGE_API))
                    .header("Content-Type", "application/json")
                    .header("Authorization", this.loopMessageKeys.getAuthorizationKey())
                    .header("Loop-Secret-Key", this.loopMessageKeys.getSecretAPIKey())
                    .POST(HttpRequest.BodyPublishers.ofString(bodyStr))
                    .build();
            HttpResponse<String> res = client.send(loopMessageRequest, HttpResponse.BodyHandlers.ofString());
            this.logger.log("STATUS CODE: " + res.statusCode());
        } catch (Exception e) {
            this.logger.log("ERROR: " + e.getMessage());
        }
        return new APIGatewayProxyResponseEvent().withStatusCode(200);
    }

    private boolean validate() {
        if (this.loopMessageKeys.getAuthorizationKey() == null || this.loopMessageKeys.getAuthorizationKey().isEmpty()) {
            this.logger.log("[ERROR] Empty Loop Authorization Key");
            return false;
        }
        if (this.loopMessageKeys.getSecretAPIKey() == null || this.loopMessageKeys.getSecretAPIKey().isEmpty()) {
            this.logger.log("[ERROR] Empty Loop Secret Key");
            return false;
        }
        String senderName = System.getenv("SENDER_NAME");
        if (senderName == null || senderName.isEmpty()) {
            this.logger.log("[ERROR] Empty SENDER_NAME");
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