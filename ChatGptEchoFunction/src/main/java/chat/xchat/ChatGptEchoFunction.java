package chat.xchat;

import chat.xchat.response.SendWhatsappMessage;
import chat.xchat.service.ChatGptService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
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
import java.util.Map;


public class ChatGptEchoFunction implements RequestHandler<Map<String, String>, APIGatewayProxyResponseEvent> {

	private final HttpClient client = HttpClient.newHttpClient();
	private final Gson gson = new Gson();
	private LambdaLogger logger;

	private ChatGptService chatGptService;

	private final SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
			.region(Region.of("us-east-1"))
			.build();

	private final String whatsappToken = getSecret("WhatsappDev");

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
			sendWhatsappMessage(phone, chatGptResponse);
		} catch (Exception e) {
			logger.log(e.getMessage());
			return response.withStatusCode(500);
		}
		return response.withStatusCode(200);
	}

	private void sendWhatsappMessage(String phone, String message) throws IOException, InterruptedException {
		String requestBody = gson.toJson(new SendWhatsappMessage(phone, message));
		logger.log("[WHATSAPP] Sending Message: " + requestBody);
		HttpResponse<String> response = client.send(HttpRequest.newBuilder()
				.uri(URI.create("https://graph.facebook.com/v16.0/" + System.getenv("WHATSAPP_PHONE_ID") + "/messages"))
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

}
