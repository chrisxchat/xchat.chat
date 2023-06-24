package chat.xchat.dto;

public class LoopMessageKeys {
    private String AuthorizationKey;
    private String SecretAPIKey;

    public String getAuthorizationKey() {
        return AuthorizationKey;
    }

    public void setAuthorizationKey(String authorizationKey) {
        AuthorizationKey = authorizationKey;
    }

    public String getSecretAPIKey() {
        return SecretAPIKey;
    }

    public void setSecretAPIKey(String secretAPIKey) {
        SecretAPIKey = secretAPIKey;
    }
}
