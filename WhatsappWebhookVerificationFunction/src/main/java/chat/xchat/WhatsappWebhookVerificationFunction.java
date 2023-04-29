package chat.xchat;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Map;

public class WhatsappWebhookVerificationFunction implements RequestHandler<Map<String, String>, APIGatewayProxyResponseEvent> {
	@Override
	public APIGatewayProxyResponseEvent handleRequest(Map<String, String> request, Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		LambdaLogger logger = context.getLogger();
		logger.log("Verification started");
		String hubMode = request.get("hub_mode");
		String hubVerifyToken = request.get("hub_verify_token");
		String hubChallenge = request.get("hub_challenge");
		if (isNotNullOrEmpty(hubMode) && isNotNullOrEmpty(hubVerifyToken)) {
			if ("subscribe".equals(hubMode) && System.getenv("WEBHOOK_VERIFICATION_TOKEN").equals(hubVerifyToken)) {
				logger.log("Verification succeeded");
				return response
						.withStatusCode(200)
						.withBody(hubChallenge);
			} else {
				logger.log("Verification failed");
				return response.withStatusCode(403);
			}
		}
		return response;
	}

	private boolean isNotNullOrEmpty(String str) {
		return str != null && !str.isEmpty();
	}
}
