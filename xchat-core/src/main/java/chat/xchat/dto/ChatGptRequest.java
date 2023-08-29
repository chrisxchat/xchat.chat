package chat.xchat.dto;


import java.util.List;

public class ChatGptRequest {

	private String model = "gpt-4";

	private List<ChatGptMessage> messages;
	private Double temperature = 0.7;
	private Integer max_tokens = 100;
	private Integer top_p = 1;
	private Integer frequency_penalty = 0;
	private Integer presence_penalty = 0;

	public ChatGptRequest(List<ChatGptMessage> messages) {
		this.messages = messages;
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

	public Integer getTop_p() {
		return top_p;
	}

	public void setTop_p(Integer top_p) {
		this.top_p = top_p;
	}

	public Integer getFrequency_penalty() {
		return frequency_penalty;
	}

	public void setFrequency_penalty(Integer frequency_penalty) {
		this.frequency_penalty = frequency_penalty;
	}

	public Integer getPresence_penalty() {
		return presence_penalty;
	}

	public void setPresence_penalty(Integer presence_penalty) {
		this.presence_penalty = presence_penalty;
	}
}
