package chat.xchat;

import chat.xchat.dto.UnansweredQuestion;
import chat.xchat.enums.Channel;
import chat.xchat.service.ChatGptService;
import chat.xchat.service.CommunicationService;
import chat.xchat.service.IMessageService;
import chat.xchat.service.QuestionService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.utils.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnansweredQuestionsHandlerFunction implements RequestHandler<Map<String, String>, APIGatewayProxyResponseEvent> {

	private LambdaLogger logger;
	private QuestionService questionService;
	private IMessageService iMessageService;
	private ChatGptService chatGptService;

	@Override
	public APIGatewayProxyResponseEvent handleRequest(Map<String, String> request, Context context) {
		this.logger = context.getLogger();
		String phone = request.get("phone");
		String email = request.get("email");
		if (StringUtils.isBlank(phone) || StringUtils.isBlank(email)) {
			this.logger.log("Empty input data");
			return new APIGatewayProxyResponseEvent().withStatusCode(500);
		}
		this.logger.log("Received data " + phone);
		this.questionService = new QuestionService(this.logger);
		List<UnansweredQuestion> unansweredQuestions = this.questionService.findUnansweredQuestions(email, phone);
		if (unansweredQuestions == null || unansweredQuestions.isEmpty()) {
			this.logger.log("Questions not found");
			return new APIGatewayProxyResponseEvent().withStatusCode(200);
		}
		this.logger.log("Found " + unansweredQuestions.size() + " unanswered question(s)");
		this.chatGptService = new ChatGptService(this.logger);
		this.logger.log("ChatGptService created");
		List<Long> ids = unansweredQuestions.parallelStream()
				.map(q -> {
					String question = q.getQuestion();
					this.logger.log("Answering question " + question);
					String answer = this.chatGptService.askChatGpt(question, null);
					this.logger.log("ChatGPT Answer: " + answer);
					getService(q.getChannel()).sendUnansweredQuestion(q, answer);
					return q.getId();
				})
				.collect(Collectors.toList());
		this.questionService.delete(ids);
		this.logger.log(ids.size() + " Unanswered questions deleted");
		return new APIGatewayProxyResponseEvent().withStatusCode(200);
	}

	private CommunicationService getService(Channel channel) {
		switch (channel) {
			case iMESSENGER: {
				if (this.iMessageService == null) {
					this.iMessageService = new IMessageService(this.logger);
				}
				return this.iMessageService;
			}
			default: {
				this.logger.log("Unsupported channel " + channel);
				throw new RuntimeException("Unsupported channel " + channel);
			}
		}

	}
}