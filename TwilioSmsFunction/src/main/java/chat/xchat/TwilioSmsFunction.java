package chat.xchat;


import chat.xchat.dto.TwilioRequest;
import chat.xchat.enums.Channel;
import chat.xchat.service.ChatGptService;
import chat.xchat.service.CommunicationService;
import chat.xchat.service.QuestionService;
import chat.xchat.service.UsersService;
import chat.xchat.service.impl.SmsCommunicationService;
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

public class TwilioSmsFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private LambdaLogger logger;
	private SmsCommunicationService smsService;
	private TwilioWhatsappCommunicationService whatsappService;

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent req, Context context) {
		this.logger = context.getLogger();

		TwilioRequest sms = extractMessage(req.getBody());
		this.logger.log("Extracted message obj " + sms);
		String message = sms.getBody();
		if (StringUtils.isBlank(message)) {
			return new APIGatewayProxyResponseEvent().withStatusCode(200);
		}
		if (StringUtils.isBlank(sms.getNumber())) {
			return new APIGatewayProxyResponseEvent().withStatusCode(400);
		}
		String phone = sms.getNumber();
		if (!new UsersService(this.logger).exists(phone)) {
			this.logger.log("Found unregistered user");
			new QuestionService(this.logger).saveUnansweredQuestion(phone, phone, message, Channel.SMS, null);
			try {
				getCommunicationService(sms).sendMessage(phone, UsersService.PLEASE_REGISTER_MESSAGE);
			} catch (Exception e) {
				this.logger.log("Unable to send 'please register' message to user " + e.getMessage());
				return new APIGatewayProxyResponseEvent().withStatusCode(500);
			}
			return new APIGatewayProxyResponseEvent().withStatusCode(200);
		}
		String chatGptResponse = null;
		try {
			chatGptResponse = new ChatGptService(this.logger).askChatGpt(phone, message, null);
			if (chatGptResponse.isBlank()) {
				throw new RuntimeException("ChatGPT response is blank");
			}
		} catch (Exception e) {
			this.logger.log("ERROR: " + e.getMessage());
			return new APIGatewayProxyResponseEvent().withStatusCode(200);
		}
		try {
			getCommunicationService(sms).sendMessage(phone, chatGptResponse);
		} catch (Exception e) {
			this.logger.log("Unable to respond to user " + e.getMessage());
			return new APIGatewayProxyResponseEvent().withStatusCode(500);
		}

		return new APIGatewayProxyResponseEvent().withStatusCode(200);
	}

	private TwilioRequest extractMessage(String encodedBody) {
		this.logger.log("Received SMS body: " + encodedBody);
		byte[] decodedBytes = Base64.getDecoder().decode(encodedBody);
		String decodedString = new String(decodedBytes);
		String[] parameters = decodedString.split("&");
		TwilioRequest request = new TwilioRequest();
		String number = extract(parameters, "From=");
		if (number.startsWith("whatsapp:+")) {
			request.setChannel(Channel.WHATSAPP);
			request.setNumber(number.replaceAll("whatsapp:\\+", ""));
		} else {
			request.setChannel(Channel.SMS);
			request.setNumber(number.replaceAll("\\+", ""));
		}
		request.setBody(extract(parameters, "Body="));
		return request;
	}

	private String extract(String[] params, String paramName) {
		return Arrays.stream(params)
				.filter(s -> s.startsWith(paramName))
				.map(s -> s.replaceAll(paramName, ""))
				.map(s -> URLDecoder.decode(s, StandardCharsets.UTF_8).strip())
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Can not find " + paramName + " in request " + Arrays.toString(params)));
	}

	private CommunicationService getCommunicationService(TwilioRequest request) {
		switch (request.getChannel()) {
			case SMS: {
				if (this.smsService == null) {
					this.smsService = new SmsCommunicationService(this.logger);
				}
				return this.smsService;
			}
			case WHATSAPP: {
				if (this.whatsappService == null) {
					this.whatsappService = new TwilioWhatsappCommunicationService(this.logger);
				}
				return this.whatsappService;
			}
		}
		throw new RuntimeException("Unable to detect communication channel");
	}

}
