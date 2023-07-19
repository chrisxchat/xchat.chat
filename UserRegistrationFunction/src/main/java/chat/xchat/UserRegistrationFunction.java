package chat.xchat;


import chat.xchat.service.UsersService;
import chat.xchat.service.impl.SmsCommunicationService;
import chat.xchat.service.impl.WhatsappCommunicationService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Map;

public class UserRegistrationFunction implements RequestHandler<Map<String, String>, APIGatewayProxyResponseEvent> {

	private LambdaLogger logger;

	private UsersService usersService;

	private final String greetingMessage = "Welcome to xChat!\n" +
			"\nThis is the first iteration of a new product that brings AI into your chat experience. Over time, we will improve this product to be trainable, internet connected, and able to perform tasks like make reservations and handle payments for you. For now, text any question or request and ChatGPT will quickly respond.";

	private final String DEVELOPER_NUMBER = "0933506675";

	@Override
	public APIGatewayProxyResponseEvent handleRequest(Map<String, String> request, Context context) {
		this.logger = context.getLogger();
		String phone = request.get("phone");
		// TODO delete later
		if (phone.contains(DEVELOPER_NUMBER)) {
			phone = "38" + DEVELOPER_NUMBER;
		} else if (!phone.startsWith("1")) {
			phone = "1" + phone;
		}
		try {
			this.usersService = new UsersService(this.logger);
			if (!usersService.exists(request.get("phone"), request.get("email"))) {
				this.usersService.save(request);
			}
			// send greeting messages
			new SmsCommunicationService(this.logger).sendMessage(phone, this.greetingMessage);
			this.logger.log("Welcome SMS sent");
			new WhatsappCommunicationService(this.logger).sendMessage(phone, this.greetingMessage);
			this.logger.log("Welcome Whatsapp message sent");
			return new APIGatewayProxyResponseEvent().withBody("User " + phone + " registered").withStatusCode(200);
		} catch (Exception e) {
			this.logger.log("ERROR: " + e.getMessage());
			return new APIGatewayProxyResponseEvent().withBody("Error").withStatusCode(200);
		}
	}

}
