package chat.xchat.service.impl;

import chat.xchat.dto.UnansweredQuestion;
import chat.xchat.dto.secrets.TwilioCredentials;
import chat.xchat.service.CommunicationService;
import chat.xchat.service.SecretService;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SmsCommunicationService implements CommunicationService {

	private LambdaLogger logger;
	private SecretService secretService;

	public SmsCommunicationService(LambdaLogger logger) {
		this.logger = logger;
		this.secretService = new SecretService();
		this.logger.log("SmsCommunicationService created");
	}

	@Override
	public void sendUnansweredQuestion(UnansweredQuestion question, String answer) {
		sendMessage(question.getChatId(), answer);
	}

	@Override
	public void sendMessage(String phone, String message) {
		TwilioCredentials twilioCredentials = secretService.getTwilioCredentials();
		Twilio.init(twilioCredentials.getTwilioAccountSID(), twilioCredentials.getTwilioAuthToken());
		Message sms = Message.creator(
						new PhoneNumber("+" + phone),
						new PhoneNumber(twilioCredentials.getTwilioSmsPhoneNumber()),
						message)
				.create();
		this.logger.log("Message SID " + sms.getSid() + ", error = " + sms.getErrorMessage());
	}
}
