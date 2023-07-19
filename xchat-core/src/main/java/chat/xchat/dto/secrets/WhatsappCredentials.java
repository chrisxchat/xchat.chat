package chat.xchat.dto.secrets;

public class WhatsappCredentials {
	private String Token;
	private String PhoneId;

	public String getToken() {
		return Token;
	}

	public void setToken(String token) {
		Token = token;
	}

	public String getPhoneId() {
		return PhoneId;
	}

	public void setPhoneId(String phoneId) {
		PhoneId = phoneId;
	}
}
