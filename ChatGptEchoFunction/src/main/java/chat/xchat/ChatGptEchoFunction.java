package chat.xchat;

import chat.xchat.dto.ChatGptRequest;
import chat.xchat.dto.ChatGptResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 * TODO:
 * 1. Store API keys securely
 * 2. Attach database to store message history
 * 3. Trigger this lambda by SNS when AWS Pinpoint phone number registered
 */
public class ChatGptEchoFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private final HttpClient client = HttpClient.newHttpClient();
	private final Gson gson = new Gson();

	public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("X-Custom-Header", "application/json");

		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
				.withHeaders(headers);
		try {
			String text = askChatGpt();
			sendSms("", text);
			return response
					.withStatusCode(200)
					.withBody(text);
		} catch (InterruptedException | IOException e) {
			return response
					.withBody("{}")
					.withStatusCode(500);
		}
	}

	private void sendSms(String phoneNumber, String text) {
		AmazonSNSClientBuilder.standard().build().publish(new PublishRequest().withPhoneNumber(phoneNumber).withMessage(text));
	}

	private String askChatGpt() throws IOException, InterruptedException {
		ChatGptRequest requestBody = new ChatGptRequest();
		requestBody.setPrompt("How to play poker?");
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.openai.com/v1/completions"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer ")
				.header("OpenAI-Organization", "")
				.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
				.build();

		HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
		return toChatGptResponse(res.body()).getChoices().get(0).getText();
	}

	private ChatGptResponse toChatGptResponse(String json) {
		if (json == null) {
			return null;
		}
		return gson.fromJson(json, ChatGptResponse.class);
	}

}
