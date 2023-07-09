package chat.xchat;

import chat.xchat.dto.LoopGroup;
import chat.xchat.dto.LoopMessageDto;
import chat.xchat.dto.LoopMessageKeys;
import chat.xchat.dto.LoopMessageRequest;
import chat.xchat.enums.Channel;
import chat.xchat.service.ChatGptService;
import chat.xchat.service.QuestionService;
import chat.xchat.service.UsersService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class LoopMessageIMessagesFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private LambdaLogger logger;
    private final Gson gson = new Gson();

    private final HttpClient client = HttpClient.newHttpClient();

    private static final String LOOP_MESSAGE_API = "https://server.loopmessage.com/api/v1/message/send/";
    private static final Integer MESSAGE_MAX_SYMBOLS = 10_000;

    private static final String PLEASE_REGISTER_MESSAGE = "Hi, I see that you’re not yet registered with xChat. Register at https://xchat.chat for free and I’ll then be able to answer your question.";

    private final SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
            .region(Region.of("us-east-1"))
            .build();

    private final LoopMessageKeys loopMessageKeys = gson.fromJson(getSecret("LoopMessageKeys"), LoopMessageKeys.class);

    private ChatGptService chatGptService;
    private UsersService usersService;
    private QuestionService questionService;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        this.logger = context.getLogger();
        this.logger.log("REQUEST: " + requestEvent.getBody());
        this.chatGptService = new ChatGptService(this.logger);
//        this.logger.log("ChatGptService created");
        this.usersService = new UsersService(this.logger);
//        this.logger.log("UsersService created");
        if (!validate()) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500);
        }
//        this.logger.log("Validation passed");
        try {
            LoopMessageDto incomingRequest = this.gson.fromJson(requestEvent.getBody(), LoopMessageDto.class);
            if (!"message_inbound".equals(incomingRequest.getAlert_type())) {
                this.logger.log("Alert type not supported");
                return new APIGatewayProxyResponseEvent().withStatusCode(200);
            }
            String recipient = incomingRequest.getRecipient();
            if (recipient.startsWith("\\+")) {
                recipient = recipient.replaceAll("\\+", "");
            }
            String senderName = incomingRequest.getSender_name();
            boolean isGroupChat = incomingRequest.getGroup() != null;
            String groupId = Optional.ofNullable(incomingRequest.getGroup()).map(LoopGroup::getGroup_id).orElse(null);
            if (isGroupChat) {
                this.logger.log("Request from group chat");
                if (containsMention(incomingRequest)) {
                    // validate if user exist
                    this.logger.log("Group chat request contains mention");
                    if (!this.usersService.exists(
                            recipient.contains("@") ? null : recipient,
                            recipient.contains("@") ? recipient : null
                    )) {
                        this.logger.log("Group chat user does not exist");
                        String bodyStr = this.gson.toJson(new LoopMessageRequest(
                           null, PLEASE_REGISTER_MESSAGE, senderName, groupId
                        ));
                        sendLoopMessage(bodyStr);
                        this.questionService = new QuestionService(this.logger);
                        this.questionService.saveUnansweredQuestion(groupId, recipient, removeMention(incomingRequest.getText()), Channel.iMESSENGER);
                        return new APIGatewayProxyResponseEvent().withStatusCode(200);
                    } else {
                        this.logger.log("CONTINUE...");
                    }
                } else {
                    this.logger.log("No bot mention, ignoring message");
                    // if no bot mentions, ignore message
                    return new APIGatewayProxyResponseEvent().withStatusCode(200);
                }
            }
            String chatGptResponse = this.chatGptService.askChatGpt(incomingRequest.getText(), MESSAGE_MAX_SYMBOLS);
            String bodyStr = this.gson.toJson(new LoopMessageRequest(
                    isGroupChat ? null : recipient,
                    chatGptResponse,
                    senderName,
                    isGroupChat ? groupId : null
            ));
            this.logger.log("RESPONSE message: " + bodyStr);
            sendLoopMessage(bodyStr);
        } catch (Exception e) {
            this.logger.log("ERROR: " + e.getMessage());
        }
        return new APIGatewayProxyResponseEvent().withStatusCode(200);
    }

    private int sendLoopMessage(String bodyStr) throws IOException, InterruptedException {
        HttpRequest loopMessageRequest = HttpRequest.newBuilder()
                .uri(URI.create(LOOP_MESSAGE_API))
                .header("Content-Type", "application/json")
                .header("Authorization", this.loopMessageKeys.getAuthorizationKey())
                .header("Loop-Secret-Key", this.loopMessageKeys.getSecretAPIKey())
                .POST(HttpRequest.BodyPublishers.ofString(bodyStr))
                .build();
        HttpResponse<String> res = client.send(loopMessageRequest, HttpResponse.BodyHandlers.ofString());
        this.logger.log("STATUS CODE: " + res.statusCode());
        return res.statusCode();
    }

    private boolean containsMention(LoopMessageDto incomingRequest) {
        return incomingRequest.getText().contains("!") || incomingRequest.getText().contains("@x") || incomingRequest.getText().contains("@xchat");
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
        return true;
    }

    private String removeMention(String text) {
        return text.replaceAll("@xchat", "").strip();
    }

    private String getSecret(String name) {
        return secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
                .secretId(name)
                .build()).secretString();
    }
}