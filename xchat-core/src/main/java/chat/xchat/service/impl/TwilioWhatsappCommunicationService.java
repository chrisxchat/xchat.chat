package chat.xchat.service.impl;

import chat.xchat.dto.UnansweredQuestion;
import chat.xchat.service.CommunicationService;
import chat.xchat.service.SecretService;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

public class TwilioWhatsappCommunicationService implements CommunicationService {

	private LambdaLogger logger;
	private SecretService secretService;

	public TwilioWhatsappCommunicationService(LambdaLogger logger) {
		this.logger = logger;
		this.secretService = new SecretService();
		this.logger.log("SmsCommunicationService created");
		Twilio.init(this.secretService.getTwilioCredentials().getTwilioAccountSID(), this.secretService.getTwilioCredentials().getTwilioAuthToken());
	}

	@Override
	public void sendUnansweredQuestion(UnansweredQuestion question, String answer) {
		sendMessage(question.getChatId(), answer);
	}

	@Override
	public void sendMessage(String phone, String message) {
		Message m = Message.creator(
						new com.twilio.type.PhoneNumber("whatsapp:+" + phone),
						new com.twilio.type.PhoneNumber("whatsapp:" + this.secretService.getTwilioCredentials().getTwilioWhatsAppPhoneNumber()),
						message)
				.create();
		this.logger.log("WhatsApp message send via Twilio " + m.getSid());
	}
}
