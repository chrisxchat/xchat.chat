package chat.xchat.dto;

import java.util.List;

public class ChatGptResponse {

	/*
	{"id":"cmpl-73NOXqOsguH0btl1VhPE3iJMGCdQl",
	"object":"text_completion",
	"created":1681039269,
	"model":"text-davinci-003",
	"choices":[
		{"text":"\n\n1. Learn the fundamentals. Before you grab a golf club,",
		 "index":0,
		 "logprobs":null,
		 "finish_reason":"length"}
	],
	"usage":{"prompt_tokens":5,"completion_tokens":15,"total_tokens":20}}
	 */
	private String id;
	private List<ChoiceDto> choices;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<ChoiceDto> getChoices() {
		return choices;
	}

	public void setChoices(List<ChoiceDto> choices) {
		this.choices = choices;
	}
}
