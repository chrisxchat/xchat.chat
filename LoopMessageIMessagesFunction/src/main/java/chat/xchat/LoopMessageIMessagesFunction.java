package chat.xchat;

import chat.xchat.dto.LoopGroup;
import chat.xchat.dto.LoopMessageDto;
import chat.xchat.dto.LoopMessageRequest;
import chat.xchat.enums.Channel;
import chat.xchat.service.ChatGptService;
import chat.xchat.service.QuestionService;
import chat.xchat.service.UsersService;
import chat.xchat.service.impl.IMessageService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

import java.util.Optional;

import static chat.xchat.service.UsersService.PLEASE_REGISTER_MESSAGE;

public class LoopMessageIMessagesFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private LambdaLogger logger;
	private final Gson gson = new Gson();

	private static final Integer MESSAGE_MAX_SYMBOLS = 10_000;

	private ChatGptService chatGptService;
	private UsersService usersService;
	private QuestionService questionService;
	private IMessageService iMessageService;

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
		this.logger = context.getLogger();
		this.logger.log("REQUEST: " + requestEvent.getBody());
		try {
			LoopMessageDto incomingRequest = this.gson.fromJson(requestEvent.getBody(), LoopMessageDto.class);
			if (!"message_inbound".equals(incomingRequest.getAlert_type())) {
				this.logger.log("Alert type not supported");
				return new APIGatewayProxyResponseEvent().withStatusCode(200);
			}
			this.chatGptService = new ChatGptService(this.logger);
			if (this.chatGptService.messageExists(incomingRequest.getMessage_id())) {
				this.logger.log("Message " + incomingRequest.getMessage_id() + " already received");
				return new APIGatewayProxyResponseEvent().withStatusCode(200);
			}
			this.logger.log("Received new message " + incomingRequest.getMessage_id());
			String recipient = incomingRequest.getRecipient();
			if (recipient.startsWith("\\+")) {
				recipient = recipient.replaceAll("\\+", "");
			}
			String senderName = incomingRequest.getSender_name();
			boolean isGroupChat = incomingRequest.getGroup() != null;
			String groupId = Optional.ofNullable(incomingRequest.getGroup()).map(LoopGroup::getGroup_id).orElse(null);
			this.iMessageService = new IMessageService(this.logger);
			if (isGroupChat) {
				this.logger.log("Request from group chat");
				if (containsMention(incomingRequest)) {
					// validate if user exist
					this.logger.log("Group chat request contains mention");
					this.usersService = new UsersService(this.logger);
					if (!this.usersService.exists(
							recipient.contains("@") ? null : recipient,
							recipient.contains("@") ? recipient : null
					)) {
						this.logger.log("Group chat user does not exist");
						LoopMessageRequest request = new LoopMessageRequest(
								null, PLEASE_REGISTER_MESSAGE, senderName, groupId
						);
						this.iMessageService.sendLoopMessage(request);
						this.questionService = new QuestionService(this.logger);
						this.questionService.saveUnansweredQuestion(groupId, recipient, removeMention(incomingRequest.getText()), Channel.iMESSENGER, senderName);
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
			String chatGptResponse = this.chatGptService.askChatGpt(
					Optional.ofNullable(groupId).orElse(recipient),
					incomingRequest.getText(),
					incomingRequest.getMessage_id(),
					MESSAGE_MAX_SYMBOLS
			);
			String bodyStr = this.gson.toJson(new LoopMessageRequest(
					isGroupChat ? null : recipient,
					chatGptResponse,
					senderName,
					isGroupChat ? groupId : null
			));
			this.logger.log("RESPONSE message: " + bodyStr);
			this.iMessageService.sendLoopMessage(bodyStr);
		} catch (Exception e) {
			this.logger.log("ERROR: " + e.getMessage());
		}
		return new APIGatewayProxyResponseEvent().withStatusCode(200);
	}

	private boolean containsMention(LoopMessageDto incomingRequest) {
		return incomingRequest.getText().contains("!") || incomingRequest.getText().contains("@x") || incomingRequest.getText().contains("@xchat");
	}

	private String removeMention(String text) {
		return text.replaceAll("@xchat", "").strip();
	}

}