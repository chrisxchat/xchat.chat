package chat.xchat.dto;

public class LoopMessageRequest {
    private String recipient;
    private String text;
    private String sender_name;
    private String group;

    public LoopMessageRequest() {
    }

    public LoopMessageRequest(String recipient, String text, String sender_name, String group) {
        this.recipient = recipient;
        this.text = text;
        this.sender_name = sender_name;
        this.group = group;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSender_name() {
        return sender_name;
    }

    public void setSender_name(String sender_name) {
        this.sender_name = sender_name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
