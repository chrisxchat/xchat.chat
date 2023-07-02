package chat.xchat.dto;

public class LoopMessageDto {
    private String alert_type;
    private String recipient;
    private String text;
    private String message_type;
    private String message_id;
    private String webhook_id;
    private String api_version;
    private String sender_name;
    private LoopGroup group;

    public String getAlert_type() {
        return alert_type;
    }

    public void setAlert_type(String alert_type) {
        this.alert_type = alert_type;
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

    public String getMessage_type() {
        return message_type;
    }

    public void setMessage_type(String message_type) {
        this.message_type = message_type;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getWebhook_id() {
        return webhook_id;
    }

    public void setWebhook_id(String webhook_id) {
        this.webhook_id = webhook_id;
    }

    public String getApi_version() {
        return api_version;
    }

    public void setApi_version(String api_version) {
        this.api_version = api_version;
    }

    public String getSender_name() {
        return sender_name;
    }

    public void setSender_name(String sender_name) {
        this.sender_name = sender_name;
    }

    public LoopGroup getGroup() {
        return group;
    }

    public void setGroup(LoopGroup group) {
        this.group = group;
    }
}
