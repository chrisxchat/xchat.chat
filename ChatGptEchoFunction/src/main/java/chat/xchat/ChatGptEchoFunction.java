package chat.xchat;

import chat.xchat.dto.ChatGptRequest;
import chat.xchat.dto.ChatGptResponse;
import chat.xchat.dto.ChoiceDto;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.util.CollectionUtils;
import com.amazonaws.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handler for requests to Lambda function.
 * TODO:
 * 1. Attach database to store message history
 * 2. Trigger this lambda by SNS when AWS Pinpoint phone number registered
 */
public class ChatGptEchoFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private final HttpClient client = HttpClient.newHttpClient();
	private final Gson gson = new Gson();

	private final SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
			.region(Region.of("us-east-1"))
			.build();

	public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("X-Custom-Header", "application/json");

		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
				.withHeaders(headers);

		Map<String, String> queryStringParameters = input.getQueryStringParameters();
		LambdaLogger logger = context.getLogger();
		if (queryStringParameters == null || queryStringParameters.isEmpty()) {
			String message = extractMessage(input.getBody());
			String phone = extractPhoneNumber(input.getBody());
			try {
				String chatGptResponse = askChatGpt(message);
				sendWhatsappMessage(chatGptResponse, phone);
			} catch (IOException | InterruptedException e) {
				logger.log(e.getMessage());
				return response
						.withStatusCode(500);
			}
			return response
					.withStatusCode(200);
		} else {
			String verifyToken = "VERIFY_TOKEN";
			String hubMode = queryStringParameters.get("hub.mode");
			String hubVerifyToken = queryStringParameters.get("hub.verify_token");
			String hubChallenge = queryStringParameters.get("hub.challenge");
			if (!StringUtils.isNullOrEmpty(hubMode) && !StringUtils.isNullOrEmpty(hubVerifyToken)) {
				if ("subscribe".equals(hubMode) && verifyToken.equals(hubVerifyToken)) {
					return response
							.withStatusCode(200)
							.withBody(hubChallenge);
				} else {
					return response.withStatusCode(403);
				}
			}
		}
		return response.withStatusCode(200);
	}

	private String extractMessage(String body) {
		JsonObject jsonObject = new JsonParser().parse(body).getAsJsonObject();
		return jsonObject.getAsJsonArray("entry").get(0).getAsJsonObject()
				.getAsJsonArray("changes").get(0).getAsJsonObject()
				.get("value").getAsJsonObject()
				.getAsJsonArray("messages").get(0).getAsJsonObject()
				.get("text").getAsJsonObject()
				.get("body").getAsString();
	}

	private String extractPhoneNumber(String body) {
		JsonObject jsonObject = new JsonParser().parse(body).getAsJsonObject();
		return jsonObject.getAsJsonArray("entry").get(0).getAsJsonObject()
				.getAsJsonArray("changes").get(0).getAsJsonObject()
				.get("value").getAsJsonObject()
				.getAsJsonArray("messages").get(0).getAsJsonObject()
				.get("from").getAsString();
	}

	private void sendWhatsappMessage(String message, String phone) throws IOException, InterruptedException {
		String requestBody = "{ \"messaging_product\": \"whatsapp\", \"to\": \"" + phone + "\", \"type\": \"text\", \"text\": { \"body\": \"" + message + "\" } }";
		client.send(HttpRequest.newBuilder()
				.uri(URI.create("https://graph.facebook.com/v16.0/106487262422291/messages"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + getWhatsappToken())
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build(), HttpResponse.BodyHandlers.ofString());
	}

	private void sendTelegramMessage(String text) throws IOException {
		String textEncoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
		String tgToken = getTelegramBotToken();
		String chatId = "-944418211";
		String urlString = "https://api.telegram.org/bot" + tgToken + "/sendMessage?chat_id=" + chatId + "&text=" + textEncoded;
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.getResponseCode();
	}

	private void sendSms(String phoneNumber, String text) {
		AmazonSNSClientBuilder.standard().build().publish(new PublishRequest().withPhoneNumber(phoneNumber).withMessage(text));
	}

	private String askChatGpt(String message) throws IOException, InterruptedException {
		ChatGptRequest requestBody = new ChatGptRequest();
		requestBody.setPrompt(message);
		requestBody.setMax_tokens(100);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.openai.com/v1/completions"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + getChatGptApiKey())
				.header("OpenAI-Organization", "org-hjmTx18GgDpomxMwhp1Gs4rP")
				.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
				.build();

		HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
		return Optional.ofNullable(toChatGptResponse(res.body()))
				.map(ChatGptResponse::getChoices)
				.map(choices -> CollectionUtils.isNullOrEmpty(choices) ? null : choices.get(0))
				.map(ChoiceDto::getText)
				.map(String::strip)
				.map(text -> text.replaceAll("\"", ""))
				.map(text -> text.replaceAll("\n", ""))
				.orElse(res.body());
	}

	private ChatGptResponse toChatGptResponse(String json) {
		if (json == null) {
			return null;
		}
		return gson.fromJson(json, ChatGptResponse.class);
	}

	private String getWhatsappToken() {
		return getSecret("WhatsappDev");
	}

	private String getChatGptApiKey() {
		return getSecret("ChatGptApiKey");
	}

	private String getTelegramBotToken() {
		return getSecret("TelegramBotTokenDev");
	}

	// TODO: check performance
	private String getSecret(String name) {
		return secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
				.secretId(name)
				.build()).secretString();
	}

}
