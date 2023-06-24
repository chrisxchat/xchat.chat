package chat.xchat.dto;

public class LoopMessageRequest {
    private String recipient;
    private String text;
    private String sender_name;

    public LoopMessageRequest() {
    }

    public LoopMessageRequest(String recipient, String text, String sender_name) {
        this.recipient = recipient;
        this.text = text;
        this.sender_name = sender_name;
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
}
