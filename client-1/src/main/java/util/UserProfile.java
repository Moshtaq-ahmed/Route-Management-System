package util;

public class UserProfile {
    private String username;
    private String fullName;
    private String uiuId;
    private String department;
    private String role;

    public UserProfile(String username, String fullName, String uiuId, String department, String role) {
        this.username = username;
        this.fullName = fullName;
        this.uiuId = uiuId;
        this.department = department;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getUiuId() { return uiuId; }
    public String getDepartment() { return department; }
    public String getRole() { return role; }
}
