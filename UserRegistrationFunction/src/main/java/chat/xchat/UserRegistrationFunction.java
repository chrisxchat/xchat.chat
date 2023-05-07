package chat.xchat;


import chat.xchat.dto.AwsSecret;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

public class UserRegistrationFunction implements RequestHandler<Map<String, String>, APIGatewayProxyResponseEvent> {

	private final Gson gson = new Gson();

	private LambdaLogger logger;

	private final SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
			.region(Region.of("us-east-1"))
			.build();

	private final AwsSecret dbCredentials = gson.fromJson(getSecret("xchat-db-dev-credentials"), AwsSecret.class);

	@Override
	public APIGatewayProxyResponseEvent handleRequest(Map<String, String> request, Context context) {
		this.logger = context.getLogger();
		try {
			String result = registerNewUser(request.get("phone"), request.get("username"));
			return new APIGatewayProxyResponseEvent().withBody(result).withStatusCode(200);
		} catch (Exception e) {
			return new APIGatewayProxyResponseEvent().withBody("Error").withStatusCode(200);
		}
	}

	private String registerNewUser(String phone, String userName) throws ClassNotFoundException {
		logger.log("[DB] Connecting");
		logger.log("[DB] database credentials acquired");
		Class.forName("com.mysql.cj.jdbc.Driver");
		try (Connection connection = DriverManager.getConnection(jdbcUrl(dbCredentials), this.dbCredentials.getUsername(), this.dbCredentials.getPassword());
		     PreparedStatement ps = connection.prepareStatement("SELECT * FROM users_data WHERE phone_number = ?")) {
			ps.setString(1, phone);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return "User already registered";
			}
			logger.log("[DB] User " + phone + " not found, creating");
			PreparedStatement createUser = connection.prepareStatement("INSERT INTO users_data (phone_number, name) VALUES (?, ?)");
			createUser.setString(1, phone);
			createUser.setString(2, userName);
			createUser.executeUpdate();
			logger.log("[DB] User saved in database");
			return "User registered";
		} catch (Exception e) {
			logger.log(e.getMessage());
			return "";
		}
	}

	private String jdbcUrl(AwsSecret dbCredentials) {
		return "jdbc:mysql://" + dbCredentials.getHost() + ":" + dbCredentials.getPort() + "/" + dbCredentials.getDatabase();
	}

	private String getSecret(String name) {
		return secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
				.secretId(name)
				.build()).secretString();
	}
}
