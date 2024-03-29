package chat.xchat.service;

import chat.xchat.dto.secrets.DatabaseSecret;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.utils.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;

public class UsersService {

	public static final String PLEASE_REGISTER_MESSAGE = "Hi, I see that you’re not yet registered with xChat. Register at https://xchat.chat for free and I’ll then be able to answer your question.";

	private final static String USER_EXIST_BY_PHONE_OR_EMAIL_SQL = "SELECT u.id FROM users_data u WHERE u.phone like ? OR u.email like ? LIMIT 1";
	private final static String USER_EXIST_BY_PHONE_SQL = "SELECT u.id FROM users_data u WHERE u.phone like ? LIMIT 1";
	private final static String CREATE_USER_SQL = "INSERT INTO users_data (phone, email, first_name, last_name) VALUES (?, ?, ?, ?)";

	private final static String PHONE = "phone";
	private final static String EMAIL = "email";
	private final static String FIRST_NAME = "firstName";
	private final static String LAST_NAME = "lastName";

	private LambdaLogger logger;

	private SecretService secretService;

	public UsersService(LambdaLogger logger) {
		this.logger = logger;
		this.secretService = new SecretService();
		this.logger.log("UsersService created");
	}

	public boolean exists(String phone) {
		if (StringUtils.isBlank(phone)) {
			this.logger.log("[UsersService] Empty phone");
			throw new RuntimeException("Empty phone");
		}
		try (Connection connection = getConnection();
			 PreparedStatement ps = connection.prepareStatement(USER_EXIST_BY_PHONE_SQL)) {
			this.logger.log("[UsersService] SQL built for phone " + phone);
			ps.setString(1, "%" + phone + "%");
			ResultSet rs = ps.executeQuery();
			return rs.next();
		} catch (Exception e) {
			this.logger.log("[UsersService] Can not check if user already registered " + e.getClass().getName() + ": " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public boolean exists(String phone, String email) {
		if (StringUtils.isBlank(phone) && StringUtils.isBlank(email)) {
			throw new RuntimeException("Empty phone and email");
		}
		try (Connection connection = getConnection();
			 PreparedStatement ps = connection.prepareStatement(USER_EXIST_BY_PHONE_OR_EMAIL_SQL)) {
			ps.setString(1, "%" + phone + "%");
			ps.setString(2, "%" + email + "%");
			ResultSet rs = ps.executeQuery();
			return rs.next();
		} catch (Exception e) {
			this.logger.log("Can not check if user already registered " + e.getClass().getName() + ": " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public void save(Map<String, String> request) {
		if (request.values().stream().map(String::strip).anyMatch(String::isBlank)) {
			this.logger.log("Can not create user with empty data: " + request);
		}
		String content = request.keySet().stream()
				.map(key -> key + "=" + request.get(key))
				.collect(Collectors.joining(", ", "{", "}"));
		this.logger.log("Trying to create user " + content);
		try (Connection connection = getConnection();
			 PreparedStatement ps = connection.prepareStatement(CREATE_USER_SQL)) {
			ps.setString(1, request.get(PHONE));
			ps.setString(2, request.get(EMAIL));
			ps.setString(3, request.get(FIRST_NAME));
			ps.setString(4, request.get(LAST_NAME));
			ps.executeUpdate();
			this.logger.log("User created in database");
		} catch (Exception e) {
			this.logger.log(String.format("Can not create user %s, %s", e.getClass().getName(), e.getMessage()));
			throw new RuntimeException(e);
		}
	}

	private Connection getConnection() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.cj.jdbc.Driver");
		DatabaseSecret databaseSecret = this.secretService.getDatabaseSecret();
		String url = "jdbc:mysql://" + databaseSecret.getProxyHost() + ":" + databaseSecret.getPort() + "/" + databaseSecret.getDatabase();
		return DriverManager.getConnection(url, databaseSecret.getUsername(), databaseSecret.getPassword());
	}

}
