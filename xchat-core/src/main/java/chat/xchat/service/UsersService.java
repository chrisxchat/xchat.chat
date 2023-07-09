package chat.xchat.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class UsersService {

    private static final String USERNAME = "username";
    private static final String PHONE = "phone";
    private static final String EMAIL = "email";

    private LambdaLogger logger;

    private final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withRegion(Regions.US_EAST_1)
            .build();

    private final String tableName = System.getenv("DYNAMO_USERS_TABLE");

    public UsersService(LambdaLogger logger) {
        this.logger = logger;
    }

    public boolean exists(String phone, String email) {
        if (StringUtils.isBlank(phone) && StringUtils.isBlank(email)) {
            throw new RuntimeException("Empty phone and email");
        }
        validate();
        ScanRequest request = new ScanRequest()
                .withTableName(this.tableName);
        if (!StringUtils.isBlank(phone)) {
            request = request.withFilterExpression("phone = :phone")
                    .withExpressionAttributeValues(Map.of("phone", new AttributeValue(email)));
        }
        if (!StringUtils.isBlank(email)) {
            request = request.withFilterExpression("email = :email")
                    .withExpressionAttributeValues(Map.of(":email", new AttributeValue(email)));
        }
        try {
            ScanResult result = this.amazonDynamoDB.scan(request);
            return result.getCount() > 0;
        } catch (Throwable e) {
            this.logger.log("User exists error: " + e.getMessage());
            return false;
        }
    }

    public void save(String username, String phone, String email) {
        validate();
        Map<String, AttributeValue> attributesMap = new HashMap<>();
        attributesMap.put(PHONE, new AttributeValue(phone));
        attributesMap.put(USERNAME, new AttributeValue(username));
        attributesMap.put(EMAIL, new AttributeValue(email));
        this.amazonDynamoDB.putItem(this.tableName, attributesMap);
    }

    private void validate() {
        if (this.tableName == null || this.tableName.isBlank()) {
            this.logger.log("[ERROR] DynamoDB table name not found");
            throw new RuntimeException("[ERROR] DynamoDB table name not found");
        }
    }

}
