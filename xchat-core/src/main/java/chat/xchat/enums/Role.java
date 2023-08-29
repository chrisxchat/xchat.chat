package chat.xchat.enums;

public enum Role {
	USER(1), ASSISTANT(2);

	public Role getRole(int id) {
		if (id == 1) {
			return USER;
		}
		return ASSISTANT;
	}

	Role(int roleId) {
		this.id = roleId;
	}

	public int getId() {
		return id;
	}

	private int id;
}
