package chat.xchat;

import chat.xchat.enums.Channel;
import chat.xchat.service.ChatGptService;
import chat.xchat.service.QuestionService;
import chat.xchat.service.UsersService;
import chat.xchat.service.impl.WhatsappCommunicationService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import software.amazon.awssdk.utils.StringUtils;

import java.util.Map;


public class WhatsappFunction implements RequestHandler<Map<String, String>, APIGatewayProxyResponseEvent> {

	private final Gson gson = new Gson();
	private LambdaLogger logger;

	private WhatsappCommunicationService whatsappService;

	public APIGatewayProxyResponseEvent handleRequest(final Map<String, String> request, final Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		this.logger = context.getLogger();
		logger.log("[INPUT] " + gson.toJson(request));
		return handleUserRequest(request, response);
	}

	private APIGatewayProxyResponseEvent handleUserRequest(Map<String, String> request, APIGatewayProxyResponseEvent response) {
		String message = request.get("message");
		if (StringUtils.isBlank(message)) {
			this.logger.log("Received empty message");
			return response.withStatusCode(200);
		}
		String phone = request.get("phone");
		if (StringUtils.isBlank(phone)) {
			this.logger.log("Received empty phone");
			return response.withStatusCode(200);
		}
		try {
			// check if user exists
			if (!new UsersService(this.logger).exists(phone)) {
				new QuestionService(this.logger).saveUnansweredQuestion(phone, phone, message, Channel.WHATSAPP, null);
				getWhatsappService().sendMessage(phone, UsersService.PLEASE_REGISTER_MESSAGE);
				return response.withStatusCode(200);
			}
			String chatGptResponse = new ChatGptService(this.logger).askChatGpt(phone, message, null);
			getWhatsappService().sendMessage(phone, chatGptResponse);
		} catch (Exception e) {
			logger.log("Can not handle whatsapp message" + e.getClass().getName() + ": " + e.getMessage());
			return response.withStatusCode(500);
		}
		return response.withStatusCode(200);
	}

	private WhatsappCommunicationService getWhatsappService() {
		if (this.whatsappService == null) {
			this.whatsappService = new WhatsappCommunicationService(this.logger);
		}
		return this.whatsappService;
	}

}
