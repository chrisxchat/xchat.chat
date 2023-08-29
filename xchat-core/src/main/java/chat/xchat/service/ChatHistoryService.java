package chat.xchat.service;

import chat.xchat.dto.ChatHistoryEntity;
import chat.xchat.dto.secrets.DatabaseSecret;
import chat.xchat.enums.Role;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryService {

	private static final int HISTORY_SIZE = 10;

	private static final String INSERT_HISTORY_RECORD_SQL = "INSERT INTO chat_history (content, role, created_date, chat_id, message_id) VALUES (?, ?, ?, ?, ?)";
	private static final String GET_HISTORY_RECORD_SQL = "SELECT id, content, role, created_date, chat_id FROM chat_history WHERE chat_id = ? ORDER BY created_date DESC LIMIT " + HISTORY_SIZE;
	private static final String MESSAGE_EXIST_SQL = "SELECT chat_id FROM chat_history WHERE message_id = ? LIMIT 1";

	private static final String ID = "id";
	private static final String CONTENT = "content";
	private static final String ROLE = "role";
	private static final String CREATED_DATE = "created_date";
	private static final String CHAT_ID = "chat_id";
	private static final String MESSAGE_ID = "message_id";

	private LambdaLogger logger;

	private SecretService secretService;

	public ChatHistoryService(LambdaLogger logger, SecretService secretService) {
		this.logger = logger;
		this.secretService = secretService;
		this.logger.log("ChatHistoryService created");
	}

	public void save(String question, String answer, String chatId, String messageId) {
		try (Connection connection = getConnection();
			 PreparedStatement ps = connection.prepareStatement(INSERT_HISTORY_RECORD_SQL)) {
			long time = System.currentTimeMillis();
			ps.setString(1, question);
			ps.setInt(2, Role.USER.getId());
			ps.setTimestamp(3, new Timestamp(time));
			ps.setString(4, chatId);
			ps.setString(5, messageId);
			ps.addBatch();
			ps.setString(1, answer);
			ps.setInt(2, Role.ASSISTANT.getId());
			ps.setTimestamp(3, new Timestamp(time + 10));
			ps.setString(4, chatId);
			ps.setString(5, messageId);
			ps.addBatch();
			ps.executeBatch();
			this.logger.log("Chat history entity saved");
		} catch (Exception e) {
			this.logger.log("Can not save chat history entity " + e.getMessage());
		}
	}

	public List<ChatHistoryEntity> getHistory(String chatId) {
		List<ChatHistoryEntity> result = new ArrayList<>(HISTORY_SIZE);
		try (Connection connection = getConnection();
			 PreparedStatement ps = connection.prepareStatement(GET_HISTORY_RECORD_SQL)) {
			ps.setString(1, chatId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				result.add(new ChatHistoryEntity(
						rs.getLong(ID),
						rs.getString(CONTENT),
						Role.USER.getRole(rs.getInt(ROLE)),
						rs.getTimestamp(CREATED_DATE),
						rs.getString(CHAT_ID)
				));
			}
		} catch (Exception e) {
			logger.log("[DB ERROR] - can not read last " + HISTORY_SIZE + " history records for chat " + chatId);
		}
		return result;
	}

	public boolean messageExists(String messageId) {
		this.logger.log("Checking duplicated message " + messageId);
		try (Connection connection = getConnection();
			 PreparedStatement ps = connection.prepareStatement(MESSAGE_EXIST_SQL)) {
			this.logger.log("SQL built");
			ps.setString(1, messageId);
			ResultSet rs = ps.executeQuery();
			return rs.next();
		} catch (Exception e) {
			logger.log("[DB ERROR] - can not check if message exist " + messageId + ", error = " + e.getMessage());
			return false;
		}
	}

	private Connection getConnection() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.cj.jdbc.Driver");
		DatabaseSecret databaseSecret = this.secretService.getDatabaseSecret();
		String url = "jdbc:mysql://" + databaseSecret.getProxyHost() + ":" + databaseSecret.getPort() + "/" + databaseSecret.getDatabase();
		return DriverManager.getConnection(url, databaseSecret.getUsername(), databaseSecret.getPassword());
	}

}
