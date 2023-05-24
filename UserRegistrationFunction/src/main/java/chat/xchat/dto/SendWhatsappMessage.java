package chat.xchat.dto;


public class SendWhatsappMessage {

	private String messaging_product = "whatsapp";
	private String to;
	private String type = "text";
	private Text text;

	public SendWhatsappMessage() {
	}

	public SendWhatsappMessage(String phone, String message) {
		this.to = phone;
		this.text = new Text(message);
	}

	public String getMessaging_product() {
		return messaging_product;
	}

	public void setMessaging_product(String messaging_product) {
		this.messaging_product = messaging_product;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Text getText() {
		return text;
	}

	public void setText(Text text) {
		this.text = text;
	}
}

class Text {
	private String body;

	public Text() {
	}

	public Text(String body) {
		this.body = body;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}