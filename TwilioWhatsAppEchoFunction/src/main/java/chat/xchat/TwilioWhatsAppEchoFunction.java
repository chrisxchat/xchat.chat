package chat.xchat;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class TwilioWhatsAppEchoFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent req, Context context) {
		LambdaLogger logger = context.getLogger();
		logger.log("==================================");
		logger.log("[APIGatewayProxyResponseEvent] " + req.getHttpMethod());
		logger.log("[APIGatewayProxyResponseEvent] " + req.getPath());
		logger.log("[APIGatewayProxyResponseEvent] " + req.getBody());
		logger.log("[APIGatewayProxyResponseEvent] " + req.getPathParameters());
		logger.log("==================================");
		return new APIGatewayProxyResponseEvent().withStatusCode(200);
	}
}
