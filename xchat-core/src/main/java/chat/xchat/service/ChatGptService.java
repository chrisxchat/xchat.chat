package chat.xchat.service;

import chat.xchat.dto.ChatGptMessage;
import chat.xchat.dto.ChatGptRequest;
import chat.xchat.dto.ChatHistoryEntity;
import chat.xchat.enums.Role;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.utils.CollectionUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Arrays.asList;

public class ChatGptService {

	private LambdaLogger logger;

	private Gson gson;

	private HttpClient client;

	private SecretService secretService;
	private ChatHistoryService chatHistoryService;

	public ChatGptService(LambdaLogger logger) {
		this.logger = logger;
		this.secretService = new SecretService();
		this.client = HttpClient.newHttpClient();
		this.gson = new Gson();
		this.chatHistoryService = new ChatHistoryService(logger, this.secretService);
		this.logger.log("ChatGptService created");
	}

	public String askChatGpt(String chatId, String message, String messageId, Integer responseLimit) {
		validateCredentials();
		String bodyStr = gson.toJson(new ChatGptRequest(getHistory(chatId, message)));
		logger.log("[ChatGPT] Asking: " + bodyStr);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.openai.com/v1/chat/completions"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + secretService.getChatGptCredentials().getApiKey())
				.header("OpenAI-Organization", secretService.getChatGptCredentials().getOrgId())
				.POST(HttpRequest.BodyPublishers.ofString(bodyStr))
				.build();
		try {
			HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
			JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
			String response = obj.getAsJsonArray("choices").get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString();
			if (responseLimit != null) {
				response = response.length() > responseLimit ? response.substring(0, responseLimit) : response;;
			}
			this.logger.log("[ChatGPT] saving question and answer");
			this.chatHistoryService.save(message, response, chatId, messageId);
			this.logger.log("[ChatGPT] question and answer saved");
			return response;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String askChatGpt(String chatId, String message, Integer responseLimit) {
		return askChatGpt(chatId, message, null, responseLimit);
	}

	private List<ChatGptMessage> getHistory(String chatId, String message) {
		ChatGptMessage systemMessage = new ChatGptMessage("system", "You are travel assistant, your name is Xchat. Please provide information in polite manner, no more than 3 sentences");
		ChatGptMessage question = new ChatGptMessage("user", message);
		this.logger.log("[ChatGPT] retrieving history for " + chatId);
		List<ChatHistoryEntity> dbHistory = this.chatHistoryService.getHistory(chatId);
		this.logger.log("[ChatGPT] history fetched, size = " + dbHistory.size());
		if (CollectionUtils.isNullOrEmpty(dbHistory)) {
			return asList(systemMessage, question);
		}
		List<ChatGptMessage> history = new ArrayList<>(dbHistory.size() + 2);
		history.add(systemMessage);
		dbHistory.stream()
				.sorted(Comparator.comparing(ChatHistoryEntity::getCreatedDate))
				.forEach(record -> history.add(new ChatGptMessage(
						Role.USER.equals(record.getRole()) ? "user" : "assistant", record.getContent()
				)));
		history.add(question);
		return history;
	}

	private void validateCredentials() {
		this.logger.log("Validating chat gpt credentials");
		try {
			String apiKey = secretService.getChatGptCredentials().getApiKey();
			if (apiKey == null || apiKey.isBlank()) {
				this.logger.log("[ERROR] Can not get ChatGPT API key");
				throw new RuntimeException("[ERROR] Can not get ChatGPT API key");
			}
			String openaiOrgId = secretService.getChatGptCredentials().getOrgId();
			if (openaiOrgId == null || openaiOrgId.isBlank()) {
				this.logger.log("[ERROR] Can not get OPENAI_ORG_ID");
				throw new RuntimeException("[ERROR] Can not get OPENAI_ORG_ID");
			}
		} catch (Throwable t) {
			this.logger.log("Exception during chat gpt credentials validation " + t.getMessage());
		}
		this.logger.log("Chat gpt credentials validated");
	}

	public boolean messageExists(String messageId) {
		return this.chatHistoryService.messageExists(messageId);
	}
}
