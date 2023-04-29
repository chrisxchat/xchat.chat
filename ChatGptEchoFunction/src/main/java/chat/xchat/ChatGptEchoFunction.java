package chat.xchat;

import chat.xchat.dto.ChatGptRequest;
import chat.xchat.response.SendWhatsappMessage;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
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
import java.util.Map;


public class ChatGptEchoFunction implements RequestHandler<Map<String, String>, APIGatewayProxyResponseEvent> {

	private final HttpClient client = HttpClient.newHttpClient();
	private final Gson gson = new Gson();
	private LambdaLogger logger;

	private final SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
			.region(Region.of("us-east-1"))
			.build();

	private final String whatsappToken = getSecret("WhatsappDev");
	private final String chatGptApiKey = getSecret("ChatGptApiKey");

	public APIGatewayProxyResponseEvent handleRequest(final Map<String, String> request, final Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		this.logger = context.getLogger();
		logger.log("[INPUT] " + gson.toJson(request));
		return handleUserRequest(request, response);
	}

	private APIGatewayProxyResponseEvent handleUserRequest(Map<String, String> request, APIGatewayProxyResponseEvent response) {
		String message = request.get("message");
		if (message == null) {
			return response.withStatusCode(200);
		}
		String phone = request.get("phone");
		try {
			if (phone.equals("380933506675") && message.equals("Read")) {
				sendWhatsappMessage("DB read mock", phone);
			} else {
				String greetingMessage = "";//greetNewUser(phone, request.get("username"));
				String chatGptResponse = askChatGpt(message);
				sendWhatsappMessage(greetingMessage + chatGptResponse, phone);
			}
		} catch (Exception e) {
			logger.log(e.getMessage());
			return response.withStatusCode(500);
		}
		return response.withStatusCode(200);
	}

//	private String greetNewUser(String phone, String userName) throws ClassNotFoundException {
//		logger.log("[DB] Connecting");
//		AwsSecret dbCredentials = getDbCredentials();
//		logger.log("[DB] database credentials acquired");
//		Class.forName("com.mysql.cj.jdbc.Driver");
//		try (Connection connection = DriverManager.getConnection(jdbcUrl(dbCredentials), dbCredentials.getUsername(), dbCredentials.getPassword());
//		     PreparedStatement ps = connection.prepareStatement("SELECT * FROM users_data WHERE phone_number = ?")) {
//			ps.setString(1, phone);
//			ResultSet rs = ps.executeQuery();
//			if (rs.next()) {
//				return "";
//			}
//			logger.log("[DB] User " + phone + " not found, creating");
//			PreparedStatement createUser = connection.prepareStatement("INSERT INTO users_data (phone_number, name) VALUES (?, ?)");
//			createUser.setString(1, phone);
//			createUser.setString(2, userName);
//			createUser.executeUpdate();
//			logger.log("[DB] User saved in database");
//			return "Welcome to xChat!\nThis is the first iteration of a new product that brings AI into your chat experience.   Over time, we'll improve this product to be trainable, internet connected, and able to perform tasks like make reservations and handle payments for you. For now, text any question or request and ChatGPT will quickly respond.\n\n";
//		} catch (Exception e) {
//			logger.log(e.getMessage());
//			return "";
//		}
//	}
//
//	private String jdbcUrl(AwsSecret dbCredentials) {
//		return "jdbc:mysql://" + dbCredentials.getHost() + ":" + dbCredentials.getPort() + "/" + dbCredentials.getDatabase();
//	}
//
//	private String readUsers() throws ClassNotFoundException {
//		logger.log("[DB] Trying to connect RDS Proxy");
//		AwsSecret dbCredentials = getDbCredentials();
//		logger.log("[DB] database credentials acquired");
//		StringBuilder sb = new StringBuilder();
//		Class.forName("com.mysql.cj.jdbc.Driver");
//		try (Connection connection = DriverManager.getConnection(jdbcUrl(dbCredentials), dbCredentials.getUsername(), dbCredentials.getPassword());
//		     PreparedStatement ps = connection.prepareStatement("SELECT * FROM users_data")) {
//			ResultSet rs = ps.executeQuery();
//			while (rs.next()) {
//				Long id = rs.getLong("id");
//				String phone = rs.getString("phone_number");
//				String name = rs.getString("name");
//				sb.append(String.format("%s, %s, %s\n", id, name, phone));
//			}
//		} catch (Exception e) {
//			logger.log(e.getMessage());
//		}
//		logger.log("[DB] Result: " + sb);
//		return sb.toString();
//	}

	private void sendWhatsappMessage(String message, String phone) throws IOException, InterruptedException {
		String requestBody = gson.toJson(new SendWhatsappMessage(phone, message));
		logger.log("[WHATSAPP] Sending Message: " + requestBody);
		logger.log("[WHATSAPP] Token acquired");
		HttpResponse<String> response = client.send(HttpRequest.newBuilder()
				.uri(URI.create("https://graph.facebook.com/v16.0/" + System.getenv("WHATSAPP_PHONE_ID") + "/messages"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + this.whatsappToken)
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build(), HttpResponse.BodyHandlers.ofString());
		logger.log("[WHATSAPP] Message response with status: " +response.statusCode() + " and body: " + response.body());
	}

	private String askChatGpt(String message) throws IOException, InterruptedException {
		String bodyStr = gson.toJson(new ChatGptRequest(message));
		logger.log("[ChatGPT] Asking: " + bodyStr);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.openai.com/v1/chat/completions"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + this.chatGptApiKey)
				.header("OpenAI-Organization", System.getenv("OPENAI_ORG_ID"))
				.POST(HttpRequest.BodyPublishers.ofString(bodyStr))
				.build();
		HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
		JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
		return obj.getAsJsonArray("choices").get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString();
	}

//	private AwsSecret getDbCredentials() {
//		String secret = getSecret("xchat-db-dev-credentials");
//		return gson.fromJson(secret, AwsSecret.class);
//	}

	private String getSecret(String name) {
		return secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
				.secretId(name)
				.build()).secretString();
	}

}
