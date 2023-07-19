package chat.xchat;


import chat.xchat.dto.SmsRequest;
import chat.xchat.service.ChatGptService;
import chat.xchat.service.impl.SmsCommunicationService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TwilioSmsFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private LambdaLogger logger;

	private ChatGptService chatGptService;

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent req, Context context) {
		this.logger = context.getLogger();
		this.chatGptService = new ChatGptService(this.logger);

		SmsRequest sms = extractMessage(req.getBody());

		String chatGptResponse = null;
		try {
			chatGptResponse = this.chatGptService.askChatGpt(sms.getBody(), null);
			if (chatGptResponse.isBlank()) {
				throw new RuntimeException("ChatGPT response is blank");
			}
		} catch (Exception e) {
			this.logger.log("ERROR: " + e.getMessage());
			return new APIGatewayProxyResponseEvent().withStatusCode(200);
		}
		new SmsCommunicationService(this.logger).sendMessage(sms.getNumber(), chatGptResponse);

		return new APIGatewayProxyResponseEvent().withStatusCode(200);
	}

//	private void sendWhatsAppMessage(String toNumber, String text) {
//		Message message = Message.creator(
//						new PhoneNumber("whatsapp:+" + toNumber),
//						new PhoneNumber("whatsapp:" + twilioCredentials.getTwilioWhatsAppPhoneNumber()),
//						text)
//				.create();
//		this.logger.log("Message SID " + message.getSid());
//	}

	private SmsRequest extractMessage(String encodedBody) {
		this.logger.log("Received SMS body: " + encodedBody);
		byte[] decodedBytes = Base64.getDecoder().decode(encodedBody);
		String decodedString = new String(decodedBytes);
		String body = decodedString.split("&Body=")[1].split("&")[0];
		body = URLDecoder.decode(body, StandardCharsets.UTF_8).strip();
		this.logger.log("[IN DATA] text: " + body);
		String number = decodedString.split("&From=%2B")[1].split("&")[0].strip();
		this.logger.log("[IN DATA] number: " + number);
		SmsRequest request = new SmsRequest();
		request.setNumber(number);
		request.setBody(body);
		return request;
	}

}
