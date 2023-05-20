package chat.xchat;


import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.HashMap;
import java.util.Map;

public class UserRegistrationFunction implements RequestHandler<Map<String, String>, APIGatewayProxyResponseEvent> {

	private LambdaLogger logger;

	private final String tableName = System.getenv("DYNAMODB_TABLE_NAME");

	private final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
			.withRegion(Regions.US_EAST_1)
			.build();


	@Override
	public APIGatewayProxyResponseEvent handleRequest(Map<String, String> request, Context context) {
		this.logger = context.getLogger();
		if (!request.containsKey("phone")) {
			this.logger.log("ERROR: Phone number not found in request body");
			return new APIGatewayProxyResponseEvent().withBody("Phone number not found").withStatusCode(200);
		}
		try {
			Map<String, AttributeValue> attributesMap = new HashMap<>();
			attributesMap.put("phone", new AttributeValue(request.get("phone")));
			attributesMap.put("username", new AttributeValue(request.get("username")));
			this.amazonDynamoDB.putItem(this.tableName, attributesMap);
			return new APIGatewayProxyResponseEvent().withBody("User registered").withStatusCode(200);
		} catch (Exception e) {
			this.logger.log("ERROR: " + e.getMessage());
			return new APIGatewayProxyResponseEvent().withBody("Error").withStatusCode(200);
		}
	}

}
