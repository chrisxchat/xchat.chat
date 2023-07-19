package chat.xchat;

import chat.xchat.service.ChatGptService;
import chat.xchat.service.impl.WhatsappCommunicationService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

import java.util.Map;


public class ChatGptEchoFunction implements RequestHandler<Map<String, String>, APIGatewayProxyResponseEvent> {

	private final Gson gson = new Gson();
	private LambdaLogger logger;

	private ChatGptService chatGptService;

	public APIGatewayProxyResponseEvent handleRequest(final Map<String, String> request, final Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		this.logger = context.getLogger();
		this.chatGptService = new ChatGptService(this.logger);
		logger.log("[INPUT] " + gson.toJson(request));
		return handleUserRequest(request, response);
	}

	private APIGatewayProxyResponseEvent handleUserRequest(Map<String, String> request, APIGatewayProxyResponseEvent response) {
		String message = request.get("message");
		if (message == null || message.isBlank()) {
			this.logger.log("Received empty message");
			return response.withStatusCode(200);
		}
		String phone = request.get("phone");
		if (phone == null || phone.isBlank()) {
			this.logger.log("Received empty phone");
			return response.withStatusCode(200);
		}
		try {
			String chatGptResponse = this.chatGptService.askChatGpt(message, null);
			new WhatsappCommunicationService(this.logger).sendMessage(phone, chatGptResponse);
		} catch (Exception e) {
			logger.log(e.getMessage());
			return response.withStatusCode(500);
		}
		return response.withStatusCode(200);
	}

}
