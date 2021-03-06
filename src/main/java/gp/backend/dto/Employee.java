package gp.backend.dto;

import lombok.Data;

import java.util.Date;
import java.util.Optional;

@Data
public class Employee {

    private String name;
    private String id;
    private String fullName;
    private Date dateOfBirth;
    private String username;
    private Optional<String> password;
    private Optional<String> profilePic;
    private String email;
    private String phone;
    private String branchId;
    private AccountType accountType;

    public enum AccountType {
        ROLE_HELP_DESK(0), ROLE_MANAGEMENT(1), ROLE_ADMIN(2);
        private int intValue;

        AccountType(int value) {
            intValue = value;
        }
    }
}
