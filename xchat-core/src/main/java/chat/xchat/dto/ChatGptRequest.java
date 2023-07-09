package chat.xchat.dto;


import java.util.Collections;
import java.util.List;

public class ChatGptRequest {

	private String model = "gpt-4";

	private List<ChatGptMessage> messages;
	private Double temperature = 0.7;
	private Integer max_tokens = 100;

	public ChatGptRequest(String message) {
		this.messages = Collections.singletonList(new ChatGptMessage(message));
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public Integer getMax_tokens() {
		return max_tokens;
	}

	public void setMax_tokens(Integer max_tokens) {
		this.max_tokens = max_tokens;
	}

	public List<ChatGptMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<ChatGptMessage> messages) {
		this.messages = messages;
	}
}

class ChatGptMessage {
	private String role = "user";
	private String content;

	public ChatGptMessage() {
	}

	public ChatGptMessage(String content) {
		this.content = content;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
