package chat.xchat.dto.secrets;


public class TwilioCredentials {
	private String TwilioAccountSID;
	private String TwilioAuthToken;
	private String TwilioSmsPhoneNumber;
	private String TwilioWhatsAppPhoneNumber;

	public String getTwilioAccountSID() {
		return TwilioAccountSID;
	}

	public void setTwilioAccountSID(String twilioAccountSID) {
		TwilioAccountSID = twilioAccountSID;
	}

	public String getTwilioAuthToken() {
		return TwilioAuthToken;
	}

	public String getTwilioWhatsAppPhoneNumber() {
		return TwilioWhatsAppPhoneNumber;
	}

	public void setTwilioWhatsAppPhoneNumber(String twilioWhatsAppPhoneNumber) {
		TwilioWhatsAppPhoneNumber = twilioWhatsAppPhoneNumber;
	}

	public void setTwilioAuthToken(String twilioAuthToken) {
		TwilioAuthToken = twilioAuthToken;
	}

	public String getTwilioSmsPhoneNumber() {
		return TwilioSmsPhoneNumber;
	}

	public void setTwilioSmsPhoneNumber(String twilioSmsPhoneNumber) {
		TwilioSmsPhoneNumber = twilioSmsPhoneNumber;
	}
}
