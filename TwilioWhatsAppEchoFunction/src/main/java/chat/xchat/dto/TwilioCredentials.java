package chat.xchat.dto;


public class TwilioCredentials {
	private String TwilioAccountSID;
	private String TwilioAuthToken;
	private String TwilioPhoneNumber;

	public String getTwilioAccountSID() {
		return TwilioAccountSID;
	}

	public void setTwilioAccountSID(String twilioAccountSID) {
		TwilioAccountSID = twilioAccountSID;
	}

	public String getTwilioAuthToken() {
		return TwilioAuthToken;
	}

	public void setTwilioAuthToken(String twilioAuthToken) {
		TwilioAuthToken = twilioAuthToken;
	}

	public String getTwilioPhoneNumber() {
		return TwilioPhoneNumber;
	}

	public void setTwilioPhoneNumber(String twilioPhoneNumber) {
		TwilioPhoneNumber = twilioPhoneNumber;
	}
}
