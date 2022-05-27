package gp.backend.data.entities;

import gp.backend.dto.Employee;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
public class EmployeeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private String id;

    @Column(unique = true)
    private String username;

    private String name;
    private String profilePic;
    private String fullName;
    private Date dateOfBirth;
    private String email;
    private String phone;
    private Employee.AccountType accountType;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    
    @ManyToOne(optional = false)
    @JoinColumn(name = "institute_id")
    private InstituteEntity institute;


}
