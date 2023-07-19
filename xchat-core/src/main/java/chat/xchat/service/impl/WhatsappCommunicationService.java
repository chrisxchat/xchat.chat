package chat.xchat.service.impl;

import chat.xchat.dto.SendWhatsappMessage;
import chat.xchat.dto.UnansweredQuestion;
import chat.xchat.dto.secrets.WhatsappCredentials;
import chat.xchat.service.CommunicationService;
import chat.xchat.service.SecretService;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WhatsappCommunicationService implements CommunicationService {

	private final Gson gson = new Gson();
	private final HttpClient client = HttpClient.newHttpClient();
	private LambdaLogger logger;
	private SecretService secretService;

	public WhatsappCommunicationService(LambdaLogger logger) {
		this.logger = logger;
		this.secretService = new SecretService();
		this.logger.log("WhatsappCommunicationService created");
	}

	@Override
	public void sendUnansweredQuestion(UnansweredQuestion question, String answer) {

	}

	@Override
	public void sendMessage(String phone, String message) throws IOException, InterruptedException {
		WhatsappCredentials whatsappCredentials = this.secretService.getWhatsappCredentials();
		String requestBody = gson.toJson(new SendWhatsappMessage(phone, message));
		logger.log("[WHATSAPP] Sending Message: " + requestBody);
		HttpResponse<String> response = client.send(HttpRequest.newBuilder()
				.uri(URI.create("https://graph.facebook.com/v16.0/" + whatsappCredentials.getPhoneId() + "/messages"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + whatsappCredentials.getToken())
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build(), HttpResponse.BodyHandlers.ofString());
		logger.log("[WHATSAPP] Message response with status: " + response.statusCode() + " and body: " + response.body());
	}
}
