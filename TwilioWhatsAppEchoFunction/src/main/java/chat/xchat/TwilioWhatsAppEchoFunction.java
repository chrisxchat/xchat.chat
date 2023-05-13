package chat.xchat;


import chat.xchat.dto.TwilioCredentials;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

public class TwilioWhatsAppEchoFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private final Gson gson = new Gson();

	private final SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
			.region(Region.of("us-east-1"))
			.build();

	private final TwilioCredentials twilioCredentials = gson.fromJson(getSecret("TwilioCredentials"), TwilioCredentials.class);

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent req, Context context) {
		LambdaLogger logger = context.getLogger();
		logger.log("==================================");
		logger.log("[APIGatewayProxyResponseEvent] " + req.getHttpMethod());
		logger.log("[APIGatewayProxyResponseEvent] " + req.getPath());
		logger.log("[APIGatewayProxyResponseEvent] " + req.getBody());
		logger.log("[APIGatewayProxyResponseEvent] " + req.getPathParameters());
		logger.log("==================================");

		Twilio.init(twilioCredentials.getTwilioAccountSID(), twilioCredentials.getTwilioAuthToken());
		Message message = Message.creator(
						new PhoneNumber("whatsapp:+380933506675"),
						new PhoneNumber("whatsapp:" + twilioCredentials.getTwilioPhoneNumber()),
						"Hello there!")
				.create();

		logger.log("Message SID " + message.getSid());

		return new APIGatewayProxyResponseEvent().withStatusCode(200);
	}

	private String getSecret(String name) {
		return secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
				.secretId(name)
				.build()).secretString();
	}

}
