package gp.backend.dto;

import lombok.Data;

import java.util.Date;

@Data
public class Employee {

    private String name;
    private String id;
    private String profilePic;
    private String fullName;
    private Date dateOfBirth;
    private String username;
    private String email;
    private String phone;
    private String branchId;

}
