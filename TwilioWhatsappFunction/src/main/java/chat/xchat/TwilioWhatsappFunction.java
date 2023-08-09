package chat.xchat;

import chat.xchat.enums.Channel;
import chat.xchat.service.ChatGptService;
import chat.xchat.service.QuestionService;
import chat.xchat.service.UsersService;
import chat.xchat.service.impl.TwilioWhatsappCommunicationService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.utils.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;


public class TwilioWhatsappFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private LambdaLogger logger;

	private TwilioWhatsappCommunicationService whatsappService;

	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		this.logger = context.getLogger();
		String in = request.getBody();
		this.logger.log("Received request " + in);
		byte[] decodedBytes = Base64.getDecoder().decode(in);
		String decodedString = new String(decodedBytes);
		String[] split = decodedString.split("&");
		String toNumber = Arrays.stream(split)
				.filter(s -> s.startsWith("WaId="))
				.map(s -> s.replaceAll("WaId=", ""))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Can not find FROM number in request " + in));
		String body = Arrays.stream(split)
				.filter(s -> s.startsWith("Body="))
				.map(s -> s.replaceAll("Body=", ""))
				.map(s -> URLDecoder.decode(s, StandardCharsets.UTF_8).strip())
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Can not find BODY in request " + in));
		if (StringUtils.isBlank(toNumber) || StringUtils.isBlank(body)) {
			this.logger.log("Not able to detect phone number or/and body in " + in);
			return response.withStatusCode(400);
		}
		try {
			// check if user exists
			if (!new UsersService(this.logger).exists(toNumber)) {
				new QuestionService(this.logger).saveUnansweredQuestion(toNumber, toNumber, body, Channel.TWILIO_WHATSAPP, null);
				getWhatsappService().sendMessage(toNumber, UsersService.PLEASE_REGISTER_MESSAGE);
				return response.withStatusCode(200);
			}
			String chatGptResponse = new ChatGptService(this.logger).askChatGpt(body, null);
			getWhatsappService().sendMessage(toNumber, chatGptResponse);
		} catch (Exception e) {
			logger.log("Can not handle whatsapp message" + e.getClass().getName() + ": " + e.getMessage());
			return response.withStatusCode(500);
		}
		return response.withStatusCode(200);
	}

	private TwilioWhatsappCommunicationService getWhatsappService() {
		if (this.whatsappService == null) {
			this.whatsappService = new TwilioWhatsappCommunicationService(this.logger);
		}
		return this.whatsappService;
	}

}
