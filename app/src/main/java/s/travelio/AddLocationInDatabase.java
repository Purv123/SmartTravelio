package s.travelio;

/**
 * Created by SANKET on 13-08-2017.
 */

public class AddLocationInDatabase {

    String userId;
    String userName;
    String phoneNumber;
    String eMail;
    String student;
    String password;
    String randomKey;

    public AddLocationInDatabase(){
    }
    public AddLocationInDatabase(String userId, String userName, String phoneNumber, String eMail, String student, String password, String randomKey) {
        this.userId = userId;
        this.userName = userName;
        this.phoneNumber = phoneNumber;
        this.eMail = eMail;
        this.student = student;
        this.password = password;
        this.randomKey = randomKey;
    }
    public AddLocationInDatabase(String phoneNumber, String password) {
    }
    public String getUserId() {
        return userId;
    }
    public String getUserName() {
        return userName;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public String geteMail() {
        return eMail;
    }
    public String getStudent() {
        return student;
    }
    public String getPassword() {
        return password;
    }
    public String getRandomKey() {
        return randomKey;
    }
}
