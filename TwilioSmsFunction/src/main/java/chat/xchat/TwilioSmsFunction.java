package chat.xchat;


import chat.xchat.dto.SmsRequest;
import chat.xchat.dto.TwilioCredentials;
import chat.xchat.service.ChatGptService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TwilioSmsFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private LambdaLogger logger;

	private final Gson gson = new Gson();

	private final SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
			.region(Region.of("us-east-1"))
			.build();

	private final TwilioCredentials twilioCredentials = gson.fromJson(getSecret("TwilioCredentials"), TwilioCredentials.class);

	private ChatGptService chatGptService;

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent req, Context context) {
		this.logger = context.getLogger();
		this.chatGptService = new ChatGptService(this.logger);

		SmsRequest sms = extractMessage(req.getBody());

		if (!validateInputDataAndCredentials(sms)) {
			return new APIGatewayProxyResponseEvent().withStatusCode(200);
		}

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

		Twilio.init(twilioCredentials.getTwilioAccountSID(), twilioCredentials.getTwilioAuthToken());
		sendSms(sms.getNumber(), chatGptResponse);

		return new APIGatewayProxyResponseEvent().withStatusCode(200);
	}

	private void sendSms(String toNumber, String text) {
		Message message = Message.creator(
						new PhoneNumber("+" + toNumber),
						new PhoneNumber(twilioCredentials.getTwilioSmsPhoneNumber()),
						text)
				.create();
		this.logger.log("Message SID " + message.getSid());
	}

	private void sendWhatsAppMessage(String toNumber, String text) {
		Message message = Message.creator(
						new PhoneNumber("whatsapp:+" + toNumber),
						new PhoneNumber("whatsapp:" + twilioCredentials.getTwilioWhatsAppPhoneNumber()),
						text)
				.create();
		this.logger.log("Message SID " + message.getSid());
	}

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

	private String getSecret(String name) {
		return secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
				.secretId(name)
				.build()).secretString();
	}

	private boolean validateInputDataAndCredentials(SmsRequest sms) {
		if (sms.isEmpty()) {
			this.logger.log("[ERROR] Empty SMS message received");
			return false;
		}
		if (twilioCredentials == null) {
			this.logger.log("[ERROR] no Twilio credentials");
			return false;
		}
//		if (twilioCredentials.getTwilioWhatsAppPhoneNumber() == null || twilioCredentials.getTwilioWhatsAppPhoneNumber().isBlank()) {
//			this.logger.log("[ERROR] no Twilio WhatsApp phone number found");
//			return false;
//		}
		if (twilioCredentials.getTwilioAuthToken() == null || twilioCredentials.getTwilioAuthToken().isBlank()) {
			this.logger.log("[ERROR] no Twilio auth token found");
			return false;
		}
		if (twilioCredentials.getTwilioAccountSID() == null || twilioCredentials.getTwilioAccountSID().isBlank()) {
			this.logger.log("[ERROR] no Twilio account SID found");
			return false;
		}
		if (twilioCredentials.getTwilioSmsPhoneNumber() == null || twilioCredentials.getTwilioSmsPhoneNumber().isBlank()) {
			this.logger.log("[ERROR] no Twilio SMS phone number found");
			return false;
		}
		return true;
	}

}
