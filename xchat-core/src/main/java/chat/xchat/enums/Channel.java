package chat.xchat.enums;

public enum Channel {
    WHATSAPP(1), SMS(2), iMESSENGER(3);

    private int id;

    Channel(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
