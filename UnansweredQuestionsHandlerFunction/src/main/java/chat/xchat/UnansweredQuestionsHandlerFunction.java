package chat.xchat;

import chat.xchat.dto.UnansweredQuestion;
import chat.xchat.enums.Channel;
import chat.xchat.service.ChatGptService;
import chat.xchat.service.CommunicationService;
import chat.xchat.service.impl.IMessageService;
import chat.xchat.service.QuestionService;
import chat.xchat.service.impl.SmsCommunicationService;
import chat.xchat.service.impl.TwilioWhatsappCommunicationService;
import chat.xchat.service.impl.WhatsappCommunicationService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.utils.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class UnansweredQuestionsHandlerFunction implements RequestHandler<Map<String, String>, APIGatewayProxyResponseEvent> {

	private LambdaLogger logger;
	private QuestionService questionService;
	private IMessageService iMessageService;
	private WhatsappCommunicationService whatsappService;
	private SmsCommunicationService smsService;
	private TwilioWhatsappCommunicationService twilioWhatsappService;
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
				.map(this::handleQuestion)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		this.questionService.delete(ids);
		this.logger.log(ids.size() + " Unanswered questions deleted");
		return new APIGatewayProxyResponseEvent().withStatusCode(200);
	}

	private Long handleQuestion(UnansweredQuestion q) {
		String question = q.getQuestion();
		this.logger.log("Answering question " + question);
		try {
			String answer = this.chatGptService.askChatGpt(q.getChatId(), question, null);
			this.logger.log("ChatGPT Answer: " + answer);
			getService(q.getChannel()).sendUnansweredQuestion(q, answer);
		} catch (Exception e) {
			this.logger.log("Can not handle question #" + q.getId());
			return null;
		}
		return q.getId();
	}

	private CommunicationService getService(Channel channel) {
		switch (channel) {
			case iMESSENGER: {
				if (this.iMessageService == null) {
					this.iMessageService = new IMessageService(this.logger);
				}
				return this.iMessageService;
			}
			case WHATSAPP: {
				if (this.whatsappService == null) {
					this.whatsappService = new WhatsappCommunicationService(this.logger);
				}
				return this.whatsappService;
			}
			case SMS: {
				if (this.smsService == null) {
					this.smsService = new SmsCommunicationService(this.logger);
				}
				return this.smsService;
			}
			case TWILIO_WHATSAPP: {
				if (this.twilioWhatsappService == null) {
					this.twilioWhatsappService = new TwilioWhatsappCommunicationService(this.logger);
				}
				return this.twilioWhatsappService;
			}
			default: {
				this.logger.log("Unsupported channel " + channel);
				throw new RuntimeException("Unsupported channel " + channel);
			}
		}

	}
}