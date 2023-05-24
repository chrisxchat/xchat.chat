package chat.xchat;


import chat.xchat.dto.SendWhatsappMessage;
import chat.xchat.dto.TwilioCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class UserRegistrationFunction implements RequestHandler<Map<String, String>, APIGatewayProxyResponseEvent> {

	private LambdaLogger logger;
	private final Gson gson = new Gson();
	private final HttpClient client = HttpClient.newHttpClient();

	private final String tableName = System.getenv("DYNAMODB_TABLE_NAME");
	private final String whatsAppPhoneId = System.getenv("WHATSAPP_PHONE_ID");

	private final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
			.withRegion(Regions.US_EAST_1)
			.build();

	private final SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
			.region(Region.of("us-east-1"))
			.build();

	private final TwilioCredentials twilioCredentials = gson.fromJson(getSecret("TwilioCredentials"), TwilioCredentials.class);

	private final String whatsappToken = getSecret("WhatsappDev");

	private final String greetingMessage = "Welcome to xChat!\n" +
			"\nThis is the first iteration of a new product that brings AI into your chat experience. Over time, we will improve this product to be trainable, internet connected, and able to perform tasks like make reservations and handle payments for you. For now, text any question or request and ChatGPT will quickly respond.";

	@Override
	public APIGatewayProxyResponseEvent handleRequest(Map<String, String> request, Context context) {
		this.logger = context.getLogger();
		String phone = request.get("phone");
		if (!validateInputDataAndCredentials(phone)) {
			return new APIGatewayProxyResponseEvent().withStatusCode(200);
		}

		try {
			GetItemRequest getUserRequest = new GetItemRequest(this.tableName, Map.of("phone", new AttributeValue(phone)));
			Map<String, AttributeValue> user = this.amazonDynamoDB.getItem(getUserRequest).getItem();
			if (user == null) {
				Map<String, AttributeValue> attributesMap = new HashMap<>();
				attributesMap.put("phone", new AttributeValue(phone));
				attributesMap.put("username", new AttributeValue(request.get("username")));
				this.amazonDynamoDB.putItem(this.tableName, attributesMap);
				// send greeting messages
				sendSms(phone, this.greetingMessage);
				sendWhatsappMessage(phone, this.greetingMessage);
				return new APIGatewayProxyResponseEvent().withBody("User registered").withStatusCode(200);
			} else {
				return new APIGatewayProxyResponseEvent().withBody("User " + phone + " already registered").withStatusCode(200);
			}
		} catch (Exception e) {
			this.logger.log("ERROR: " + e.getMessage());
			return new APIGatewayProxyResponseEvent().withBody("Error").withStatusCode(200);
		}
	}

	private void sendSms(String toNumber, String text) {
		Twilio.init(twilioCredentials.getTwilioAccountSID(), twilioCredentials.getTwilioAuthToken());
		Message message = Message.creator(
						new PhoneNumber("+" + toNumber),
						new PhoneNumber(twilioCredentials.getTwilioSmsPhoneNumber()),
						text)
				.create();
		this.logger.log("Message SID " + message.getSid());
	}

	private void sendWhatsappMessage(String phone, String message) throws IOException, InterruptedException {
		String requestBody = gson.toJson(new SendWhatsappMessage(phone, message));
		logger.log("[WHATSAPP] Sending Message: " + requestBody);
		HttpResponse<String> response = client.send(HttpRequest.newBuilder()
				.uri(URI.create("https://graph.facebook.com/v16.0/" + this.whatsAppPhoneId + "/messages"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + this.whatsappToken)
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build(), HttpResponse.BodyHandlers.ofString());
		logger.log("[WHATSAPP] Message response with status: " +response.statusCode() + " and body: " + response.body());
	}

	private String getSecret(String name) {
		return secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
				.secretId(name)
				.build()).secretString();
	}

	private boolean validateInputDataAndCredentials(String phone) {
		if (this.tableName == null || this.tableName.isBlank()) {
			this.logger.log("[ERROR] DynamoDB table name not found");
			return false;
		}
		if (this.whatsappToken == null || this.whatsappToken.isBlank()) {
			this.logger.log("[ERROR] WhatsApp token not found");
			return false;
		}
		if (this.whatsAppPhoneId == null || this.whatsAppPhoneId.isBlank()) {
			this.logger.log("[ERROR] WhatsApp phone id not found");
			return false;
		}
		if (phone == null || phone.isBlank()) {
			this.logger.log("[ERROR] Phone number not found in request body");
			return false;
		}
		if (twilioCredentials == null) {
			this.logger.log("[ERROR] no Twilio credentials");
			return false;
		}
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
