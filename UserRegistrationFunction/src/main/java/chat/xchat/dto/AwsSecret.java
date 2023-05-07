package chat.xchat.dto;

public class AwsSecret {
	private String username;
	private String password;
	private String host;
	private String engine;
	private String port;
	private String dbInstanceIdentifier;
	private String database;

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getHost() {
		return host;
	}

	public String getEngine() {
		return engine;
	}

	public String getPort() {
		return port;
	}

	public String getDbInstanceIdentifier() {
		return dbInstanceIdentifier;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}
}
