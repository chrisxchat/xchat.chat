package chat.xchat.service.impl;

import chat.xchat.dto.LoopMessageKeys;
import chat.xchat.dto.LoopMessageRequest;
import chat.xchat.dto.UnansweredQuestion;
import chat.xchat.service.CommunicationService;
import chat.xchat.service.SecretService;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class IMessageService implements CommunicationService {

	private static final String LOOP_MESSAGE_API = "https://server.loopmessage.com/api/v1/message/send/";
	private static final Integer MESSAGE_MAX_SYMBOLS = 10_000;

	private final Gson gson = new Gson();

	private final HttpClient client = HttpClient.newHttpClient();

	private LambdaLogger logger;
	private SecretService secretService;

	public IMessageService(LambdaLogger logger) {
		this.logger = logger;
		this.secretService = new SecretService();
		this.logger.log("IMessageService created");
	}

	public void sendUnansweredQuestion(UnansweredQuestion question, String answer) {
		LoopMessageRequest request = new LoopMessageRequest();
		request.setText(answer);
		request.setGroup(question.getChatId());
		request.setRecipient(question.getUser());
		request.setSender_name(question.getSenderName());
		sendLoopMessage(request);
	}

	@Override
	public void sendMessage(String phone, String message) {

	}

	public int sendLoopMessage(LoopMessageRequest request) {
		String bodyStr = this.gson.toJson(request);
		return sendLoopMessage(bodyStr);
	}

	public int sendLoopMessage(String bodyStr) {
		LoopMessageKeys loopMessageKeys = secretService.getLoopMessageKeys();
		HttpRequest loopMessageRequest = HttpRequest.newBuilder()
				.uri(URI.create(LOOP_MESSAGE_API))
				.header("Content-Type", "application/json")
				.header("Authorization", loopMessageKeys.getAuthorizationKey())
				.header("Loop-Secret-Key", loopMessageKeys.getSecretAPIKey())
				.POST(HttpRequest.BodyPublishers.ofString(bodyStr))
				.build();
		try {
			HttpResponse<String> res = client.send(loopMessageRequest, HttpResponse.BodyHandlers.ofString());
			this.logger.log("STATUS CODE: " + res.statusCode());
			return res.statusCode();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
