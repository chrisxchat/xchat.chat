package chat.xchat.dto;

import chat.xchat.enums.Role;

import java.util.Date;

public class ChatHistoryEntity {
	private Long id;
	private String content;
	private Role role;
	private Date createdDate;
	private String chatId;

	public ChatHistoryEntity() {
	}

	public ChatHistoryEntity(Long id, String content, Role role, Date date, String chatId) {
		this.id = id;
		this.content = content;
		this.role = role;
		this.createdDate = date;
		this.chatId = chatId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public String getChatId() {
		return chatId;
	}

	public void setChatId(String chatId) {
		this.chatId = chatId;
	}
}
