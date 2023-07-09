package chat.xchat.service;

import chat.xchat.enums.Channel;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestionService {

    private static final String ID = "id";
    private static final String CHAT_ID = "chat_id";
    private static final String USER = "user";
    private static final String QUESTION = "question";
    private static final String CHANNEL = "channel";

    private LambdaLogger logger;

    private final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withRegion(Regions.US_EAST_1)
            .build();

    private final String tableName = System.getenv("DYNAMO_UNANSWERED_QUESTIONS_TABLE");

    public QuestionService(LambdaLogger logger) {
        this.logger = logger;
    }

    public void saveUnansweredQuestion(String chatId, String user, String question, Channel channel) {
        validate();
        Map<String, AttributeValue> attributesMap = new HashMap<>();
        attributesMap.put(ID, new AttributeValue(UUID.randomUUID().toString()));
        attributesMap.put(CHAT_ID, new AttributeValue(chatId));
        attributesMap.put(USER, new AttributeValue(user));
        attributesMap.put(QUESTION, new AttributeValue(question));
        attributesMap.put(CHANNEL, new AttributeValue(channel.name()));
        this.amazonDynamoDB.putItem(this.tableName, attributesMap);
    }

    private void validate() {
        if (this.tableName == null || this.tableName.isBlank()) {
            this.logger.log("[ERROR] DynamoDB table name not found");
            throw new RuntimeException("[ERROR] DynamoDB table name not found");
        }
    }

}
