package chat.xchat;

import chat.xchat.dto.AwsSecret;
import chat.xchat.dto.ChatGptRequest;
import chat.xchat.response.SendWhatsappMessage;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class ChatGptEchoFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private final HttpClient client = HttpClient.newHttpClient();
	private final Gson gson = new Gson();
	private LambdaLogger logger;

	private final SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
			.region(Region.of("us-east-1"))
			.build();

	public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("X-Custom-Header", "application/json");

		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
				.withHeaders(headers);
		this.logger = context.getLogger();

		Map<String, String> queryParams = input.getQueryStringParameters();
		logger.log("Request body: " + input.getBody());
		if (queryParams == null || queryParams.isEmpty()) {
			return handleUserRequest(input, response);
		}
		return establishWebhookConnection(response, queryParams);
	}

	private APIGatewayProxyResponseEvent handleUserRequest(APIGatewayProxyRequestEvent input, APIGatewayProxyResponseEvent response) {
		String message = extractMessage(input.getBody());
		if (message == null) {
			return response.withStatusCode(200);
		}
		String phone = extractPhoneNumber(input.getBody());
		try {
			if (phone.equals("380933506675") && message.equals("Read")) {
				sendWhatsappMessage(readUsers(), phone);
			} else {
				String greetingMessage = greetNewUser(phone, extractUserName(input.getBody()));
				String chatGptResponse = askChatGpt(message);
				sendWhatsappMessage(greetingMessage + chatGptResponse, phone);
			}
		} catch (Exception e) {
			logger.log(e.getMessage());
			return response.withStatusCode(500);
		}
		return response.withStatusCode(200);
	}

	private APIGatewayProxyResponseEvent establishWebhookConnection(APIGatewayProxyResponseEvent response, Map<String, String> queryParams) {
		logger.log("VERIFICATION");
		String hubMode = queryParams.get("hub.mode");
		String hubVerifyToken = queryParams.get("hub.verify_token");
		String hubChallenge = queryParams.get("hub.challenge");
		if (!StringUtils.isNullOrEmpty(hubMode) && !StringUtils.isNullOrEmpty(hubVerifyToken)) {
			if ("subscribe".equals(hubMode) && System.getenv("WEBHOOK_VERIFICATION_TOKEN").equals(hubVerifyToken)) {
				return response
						.withStatusCode(200)
						.withBody(hubChallenge);
			} else {
				return response.withStatusCode(403);
			}
		}
		return response;
	}

	private String greetNewUser(String phone, String userName) throws ClassNotFoundException {
		logger.log("[DB] Connecting");
		AwsSecret dbCredentials = getDbCredentials();
		logger.log("[DB] database credentials acquired");
		Class.forName("com.mysql.cj.jdbc.Driver");
		try (Connection connection = DriverManager.getConnection(jdbcUrl(dbCredentials), dbCredentials.getUsername(), dbCredentials.getPassword());
		     PreparedStatement ps = connection.prepareStatement("SELECT * FROM users_data WHERE phone_number = ?")) {
			ps.setString(1, phone);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return "";
			}
			logger.log("[DB] User " + phone + " not found, creating");
			PreparedStatement createUser = connection.prepareStatement("INSERT INTO users_data (phone_number, name) VALUES (?, ?)");
			createUser.setString(1, phone);
			createUser.setString(2, userName);
			createUser.executeUpdate();
			logger.log("[DB] User saved in database");
			return "Welcome to xChat!\nThis is the first iteration of a new product that brings AI into your chat experience.   Over time, we'll improve this product to be trainable, internet connected, and able to perform tasks like make reservations and handle payments for you. For now, text any question or request and ChatGPT will quickly respond.\n\n";
		} catch (Exception e) {
			logger.log(e.getMessage());
			return "";
		}
	}

	private String jdbcUrl(AwsSecret dbCredentials) {
		return "jdbc:mysql://" + dbCredentials.getHost() + ":" + dbCredentials.getPort() + "/" + dbCredentials.getDatabase();
	}

	private String readUsers() throws ClassNotFoundException {
		logger.log("[DB] Trying to connect RDS Proxy");
		AwsSecret dbCredentials = getDbCredentials();
		logger.log("[DB] database credentials acquired");
		StringBuilder sb = new StringBuilder();
		Class.forName("com.mysql.cj.jdbc.Driver");
		try (Connection connection = DriverManager.getConnection(jdbcUrl(dbCredentials), dbCredentials.getUsername(), dbCredentials.getPassword());
		     PreparedStatement ps = connection.prepareStatement("SELECT * FROM users_data")) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Long id = rs.getLong("id");
				String phone = rs.getString("phone_number");
				String name = rs.getString("name");
				sb.append(String.format("%s, %s, %s\n", id, name, phone));
			}
		} catch (Exception e) {
			logger.log(e.getMessage());
		}
		logger.log("[DB] Result: " + sb);
		return sb.toString();
	}

	private String extractUserName(String body) {
		JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();
		return jsonObject.getAsJsonArray("entry").get(0).getAsJsonObject()
				.getAsJsonArray("changes").get(0).getAsJsonObject()
				.get("value").getAsJsonObject()
				.getAsJsonArray("contacts").get(0).getAsJsonObject()
				.get("profile").getAsJsonObject()
				.get("name").getAsString();
	}

	private String extractMessage(String body) {
		JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();
		return Optional.ofNullable(jsonObject.getAsJsonArray("entry").get(0).getAsJsonObject()
				.getAsJsonArray("changes").get(0).getAsJsonObject()
				.get("value").getAsJsonObject())
				.map(it -> it.getAsJsonArray("messages"))
				.map(it -> it.get(0))
				.map(JsonElement::getAsJsonObject)
				.map(it -> it.get("text"))
				.map(JsonElement::getAsJsonObject)
				.map(it -> it.get("body"))
				.map(JsonElement::getAsString)
				.orElse(null);
	}

	private String extractPhoneNumber(String body) {
		JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();
		return jsonObject.getAsJsonArray("entry").get(0).getAsJsonObject()
				.getAsJsonArray("changes").get(0).getAsJsonObject()
				.get("value").getAsJsonObject()
				.getAsJsonArray("messages").get(0).getAsJsonObject()
				.get("from").getAsString();
	}

	private void sendWhatsappMessage(String message, String phone) throws IOException, InterruptedException {
		String requestBody = gson.toJson(new SendWhatsappMessage(phone, message));
		logger.log("[WHATSAPP] Sending Message: " + requestBody);
		String whatsappToken = getWhatsappToken();
		logger.log("[WHATSAPP] Token acquired");
		HttpResponse<String> response = client.send(HttpRequest.newBuilder()
				.uri(URI.create("https://graph.facebook.com/v16.0/" + System.getenv("WHATSAPP_PHONE_ID") + "/messages"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + whatsappToken)
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build(), HttpResponse.BodyHandlers.ofString());
		logger.log("[WHATSAPP] Message response with status: " +response.statusCode() + " and body: " + response.body());
	}

	private String askChatGpt(String message) throws IOException, InterruptedException {
		String bodyStr = gson.toJson(new ChatGptRequest(message));
		logger.log("[ChatGPT] Asking: " + bodyStr);
		String chatGptApiKey = getChatGptApiKey();
		logger.log("[ChatGPT] Token acquired");
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.openai.com/v1/chat/completions"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + chatGptApiKey)
				.header("OpenAI-Organization", System.getenv("OPENAI_ORG_ID"))
				.POST(HttpRequest.BodyPublishers.ofString(bodyStr))
				.build();
		HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
		JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
		return obj.getAsJsonArray("choices").get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString();
	}

	private String getWhatsappToken() {
		return getSecret("WhatsappDev");
	}

	private String getChatGptApiKey() {
		return getSecret("ChatGptApiKey");
	}

	private AwsSecret getDbCredentials() {
		String secret = getSecret("xchat-db-dev-credentials");
		return gson.fromJson(secret, AwsSecret.class);
	}

	// TODO: check performance
	private String getSecret(String name) {
		return secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
				.secretId(name)
				.build()).secretString();
	}

}
