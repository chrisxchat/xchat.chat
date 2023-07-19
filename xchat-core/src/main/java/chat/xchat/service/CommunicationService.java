package chat.xchat.service;

import chat.xchat.dto.UnansweredQuestion;

public interface CommunicationService {

    void sendUnansweredQuestion(UnansweredQuestion question, String answer);

}
