package chat.xchat.service;

import chat.xchat.dto.ChatGptCredentials;
import chat.xchat.dto.LoopMessageKeys;
import chat.xchat.dto.secrets.DatabaseSecret;
import chat.xchat.dto.secrets.TwilioCredentials;
import chat.xchat.dto.secrets.WhatsappCredentials;
import com.google.gson.Gson;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

public class SecretService {

	private static final String WHATSAPP_CREDENTIALS = "WhatsappCredentials";
	private static final String LOOP_MESSAGE_CREDENTIALS = "LoopMessageKeys";
	private static final String TWILIO_CREDENTIALS = "TwilioCredentials";
	private static final String CHAT_GPT_CREDENTIALS = "ChatGptCredentials";
	private static final String DATABASE_SECRET = "DatabaseSecret";
	private static final String DATABASE_PROXY_HOST = "DATABASE_PROXY_HOST";
	private static final String DATABASE_PORT = "DATABASE_PORT";
	private static final String DATABASE_PASSWORD = "DATABASE_PASSWORD";
	private static final String DATABASE_USERNAME = "DATABASE_USERNAME";
	private static final String DATABASE_NAME = "DATABASE_NAME";
	private static final String CHAT_GPT_API_KEY = "CHAT_GPT_API_KEY";
	private static final String CHAT_GPT_ORG_ID = "CHAT_GPT_ORG_ID";
	private static final String LOOP_MESSAGE_AUTH_KEY = "LOOP_MESSAGE_AUTH_KEY";
	private static final String LOOP_MESSAGE_SECRET_API_KEY = "LOOP_MESSAGE_SECRET_API_KEY";
	private static final String TWILIO_ACCOUNT_SID = "TWILIO_ACCOUNT_SID";
	private static final String TWILIO_AUTH_TOKEN = "TWILIO_AUTH_TOKEN";
	private static final String TWILIO_SMS_PHONE_NUMBER = "TWILIO_SMS_PHONE_NUMBER";
	private static final String TWILIO_WHATSAPP_PHONE_NUMBER = "TWILIO_WHATSAPP_PHONE_NUMBER";
	private static final String WHATSAPP_TOKEN = "WHATSAPP_TOKEN";
	private static final String WHATSAPP_PHONE_ID = "WHATSAPP_PHONE_ID";

	private final Gson gson = new Gson();
	private SecretsManagerClient secretsManagerClient;

	private ChatGptCredentials chatGptCredentials;
	private DatabaseSecret databaseSecret;
	private WhatsappCredentials whatsappCredentials;
	private TwilioCredentials twilioCredentials;
	private LoopMessageKeys loopMessageKeys;

	public LoopMessageKeys getLoopMessageKeys() {
		if (this.loopMessageKeys == null) {
			if (System.getenv(LOOP_MESSAGE_AUTH_KEY) == null) {
				this.loopMessageKeys = gson.fromJson(getSecret(LOOP_MESSAGE_CREDENTIALS), LoopMessageKeys.class);
			} else {
				this.loopMessageKeys = getLoopMessageKeysFromEnv();
			}
		}
		return this.loopMessageKeys;
	}

	public TwilioCredentials getTwilioCredentials() {
		if (this.twilioCredentials == null) {
			if (System.getenv(TWILIO_ACCOUNT_SID) == null) {
				this.twilioCredentials = gson.fromJson(getSecret(TWILIO_CREDENTIALS), TwilioCredentials.class);
			} else {
				this.twilioCredentials = getTwilioKeysFromEnv();
			}
		}
		return this.twilioCredentials;
	}

	public WhatsappCredentials getWhatsappCredentials() {
		if (this.whatsappCredentials == null) {
			if (System.getenv(WHATSAPP_TOKEN) == null) {
				this.whatsappCredentials = gson.fromJson(getSecret(WHATSAPP_CREDENTIALS), WhatsappCredentials.class);
			} else {
				this.whatsappCredentials = getWhatsappKeysFromEnv();
			}
		}
		return this.whatsappCredentials;
	}

	public ChatGptCredentials getChatGptCredentials() {
		if (this.chatGptCredentials == null) {
			if (System.getenv(CHAT_GPT_API_KEY) == null) {
				this.chatGptCredentials = gson.fromJson(getSecret(CHAT_GPT_CREDENTIALS), ChatGptCredentials.class);
			} else {
				this.chatGptCredentials = getChatGptCredentialsFromEnv();
			}
		}
		return this.chatGptCredentials;
	}

	public DatabaseSecret getDatabaseSecret() {
		if (this.databaseSecret == null) {
			if (System.getenv(DATABASE_PROXY_HOST) == null) {
				this.databaseSecret = gson.fromJson(getSecret(DATABASE_SECRET), DatabaseSecret.class);
			} else {
				this.databaseSecret = getDatabaseSecretFromEnv();
			}
		}
		return this.databaseSecret;
	}

	private String getSecret(String name) {
		return getSecretsManagerClient().getSecretValue(GetSecretValueRequest.builder()
				.secretId(name)
				.build()).secretString();
	}

	private SecretsManagerClient getSecretsManagerClient() {
		if (this.secretsManagerClient == null) {
			this.secretsManagerClient = SecretsManagerClient.builder()
					.region(Region.of("us-east-1"))
					.build();
		}
		return this.secretsManagerClient;
	}

	private DatabaseSecret getDatabaseSecretFromEnv() {
		DatabaseSecret result = new DatabaseSecret();
		result.setDatabase(System.getenv(DATABASE_NAME));
		result.setUsername(System.getenv(DATABASE_USERNAME));
		result.setPassword(System.getenv(DATABASE_PASSWORD));
		result.setPort(System.getenv(DATABASE_PORT));
		result.setProxyHost(System.getenv(DATABASE_PROXY_HOST));
		return result;
	}

	private ChatGptCredentials getChatGptCredentialsFromEnv() {
		ChatGptCredentials result = new ChatGptCredentials();
		result.setApiKey(System.getenv(CHAT_GPT_API_KEY));
		result.setOrgId(System.getenv(CHAT_GPT_ORG_ID));
		return result;
	}

	private LoopMessageKeys getLoopMessageKeysFromEnv() {
		var result = new LoopMessageKeys();
		result.setAuthorizationKey(System.getenv(LOOP_MESSAGE_AUTH_KEY));
		result.setSecretAPIKey(System.getenv(LOOP_MESSAGE_SECRET_API_KEY));
		return result;
	}

	private TwilioCredentials getTwilioKeysFromEnv() {
		var res = new TwilioCredentials();
		res.setTwilioAccountSID(System.getenv(TWILIO_ACCOUNT_SID));
		res.setTwilioAuthToken(System.getenv(TWILIO_AUTH_TOKEN));
		res.setTwilioSmsPhoneNumber(System.getenv(TWILIO_SMS_PHONE_NUMBER));
		res.setTwilioWhatsAppPhoneNumber(System.getenv(TWILIO_WHATSAPP_PHONE_NUMBER));
		return res;
	}

	private WhatsappCredentials getWhatsappKeysFromEnv() {
		var res = new WhatsappCredentials();
		res.setToken(System.getenv(WHATSAPP_TOKEN));
		res.setPhoneId(System.getenv(WHATSAPP_PHONE_ID));
		return res;
	}

}
