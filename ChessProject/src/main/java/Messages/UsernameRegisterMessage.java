package Messages;

/**
 * Message class for username registration requests and responses
 */
public class UsernameRegisterMessage implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    
    private String requestedUsername;
    private String registeredUsername; // null if registration failed
    private boolean success;
    private String errorMessage;
    
    // Constructor for request
    public UsernameRegisterMessage(String requestedUsername) {
        this.requestedUsername = requestedUsername;
        this.success = false;
        this.registeredUsername = null;
        this.errorMessage = null;
    }
    
    // Constructor for response
    public UsernameRegisterMessage(String requestedUsername, String registeredUsername, boolean success, String errorMessage) {
        this.requestedUsername = requestedUsername;
        this.registeredUsername = registeredUsername;
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    // Getters
    public String getRequestedUsername() {
        return requestedUsername;
    }
    
    public String getRegisteredUsername() {
        return registeredUsername;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    // Setters for response
    public void setRegisteredUsername(String registeredUsername) {
        this.registeredUsername = registeredUsername;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
