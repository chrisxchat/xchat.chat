package chat.xchat.service;

import chat.xchat.dto.UnansweredQuestion;
import chat.xchat.dto.secrets.DatabaseSecret;
import chat.xchat.enums.Channel;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class QuestionService {

	private static final String CREATE_QUESTION_SQL = "INSERT INTO unanswered_questions (user_contact, question, channel, chat_id, sender_name) VALUES (?, ?, ?, ?, ?)";
	private static final String SELECT_QUESTION_BY_USER_CONTACT_SQL = "SELECT * FROM unanswered_questions WHERE user_contact IN (%s)";
	private static final String DELETE_QUESTIONS_SQL = "DELETE FROM unanswered_questions WHERE id IN (%s)";

	private static final String ID = "id";
	private static final String CHAT_ID = "chat_id";
	private static final String USER_CONTACT = "user_contact";
	private static final String QUESTION = "question";
	private static final String CHANNEL = "channel";
	private static final String SENDER_NAME = "sender_name";

	private LambdaLogger logger;

	private SecretService secretService;

	public QuestionService(LambdaLogger logger) {
		this.logger = logger;
		this.secretService = new SecretService();
	}

	public void saveUnansweredQuestion(String chatId, String user, String question, Channel channel, String senderName) {
		try (Connection connection = getConnection();
			 PreparedStatement ps = connection.prepareStatement(CREATE_QUESTION_SQL)) {
			ps.setString(1, user);
			ps.setString(2, question);
			ps.setInt(3, channel.getId());
			ps.setString(4, chatId);
			ps.setString(5, senderName);
			ps.executeUpdate();
			this.logger.log("Question saved");
		} catch (Exception e) {
			this.logger.log("Can not save question " + e.getMessage());
		}
	}

	public List<UnansweredQuestion> findUnansweredQuestions(String email, String phone) {
		List<UnansweredQuestion> result = new ArrayList<>();
		String sql = String.format(SELECT_QUESTION_BY_USER_CONTACT_SQL, String.join(",", Collections.nCopies(2, "?")));
		try (Connection connection = getConnection();
			 PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, email);
			ps.setString(2, phone);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				UnansweredQuestion q = new UnansweredQuestion();
				q.setId(rs.getLong(ID));
				q.setUser(rs.getString(USER_CONTACT));
				q.setQuestion(rs.getString(QUESTION));
				var channelId = rs.getInt(CHANNEL);
				q.setChannel(Arrays.stream(Channel.values()).filter(it -> it.getId() == channelId).findFirst().orElse(null));
				q.setChatId(rs.getString(CHAT_ID));
				q.setSenderName(rs.getString(SENDER_NAME));
				result.add(q);
			}
		} catch (Exception e) {
			logger.log("[DB ERROR] - can not read questions by email = " + email + " or phone = " + phone + ", error = " + e.getMessage());
		}
		return result;
	}

	public void delete(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}
		String sql = String.format(DELETE_QUESTIONS_SQL, String.join(",", Collections.nCopies(ids.size(), "?")));
		this.logger.log("Deleting with SQL: " + sql);
		try (Connection connection = getConnection();
			 PreparedStatement ps = connection.prepareStatement(sql)) {
			for (int i = 0; i < ids.size(); i++) {
				ps.setLong(i + 1, ids.get(i));
			}
			this.logger.log("Deleted " + ps.executeUpdate() + " questions");
		} catch (Exception e) {
			this.logger.log("Can not delete questions " + e.getClass().getName() + ": " + e.getMessage());
		}
	}

	private Connection getConnection() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.cj.jdbc.Driver");
		DatabaseSecret databaseSecret = this.secretService.getDatabaseSecret();
		String url = "jdbc:mysql://" + databaseSecret.getProxyHost() + ":" + databaseSecret.getPort() + "/" + databaseSecret.getDatabase();
		return DriverManager.getConnection(url, databaseSecret.getUsername(), databaseSecret.getPassword());
	}

}
