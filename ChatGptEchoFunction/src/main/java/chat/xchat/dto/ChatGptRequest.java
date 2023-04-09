package chat.xchat.dto;


public class ChatGptRequest {

	/*
	"{\"model\": \"text-davinci-003\", \"prompt\": \"how to play golf?\", \"temperature\": 1, \"max_tokens\": 15}"
	 */
	private String model = "text-davinci-003";
	private String prompt;
	private Integer temperature = 1;
	private Integer max_tokens = 15;

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public Integer getTemperature() {
		return temperature;
	}

	public void setTemperature(Integer temperature) {
		this.temperature = temperature;
	}

	public Integer getMax_tokens() {
		return max_tokens;
	}

	public void setMax_tokens(Integer max_tokens) {
		this.max_tokens = max_tokens;
	}
}
