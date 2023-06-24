package chat.xchat.dto;


public class SmsRequest {
	private String body;
	private String number;

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public boolean isEmpty() {
		return body == null || number == null || body.isBlank() || number.isBlank();
	}
}
