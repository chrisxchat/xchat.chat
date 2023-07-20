package chat.xchat.service;

import chat.xchat.dto.UnansweredQuestion;

import java.io.IOException;

public interface CommunicationService {

	void sendUnansweredQuestion(UnansweredQuestion question, String answer) throws IOException, InterruptedException;

	void sendMessage(String phone, String message) throws IOException, InterruptedException;

}
