package chat.xchat.dto;


import chat.xchat.enums.Channel;

public class TwilioRequest {
	private String body;
	private String number;
	private Channel channel;

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

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public boolean isEmpty() {
		return body == null || number == null || body.isBlank() || number.isBlank();
	}

	@Override
	public String toString() {
		return "SmsRequest{" +
				"body='" + body + '\'' +
				", number='" + number + '\'' +
				", channel=" + channel.name() +
				'}';
	}
}
