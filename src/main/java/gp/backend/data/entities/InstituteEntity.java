package gp.backend.data.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
public class InstituteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(unique = true)
    private String name;
    private String logoUrl;
    private String email;
    private String phone;

    private boolean admin;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "institute", cascade = CascadeType.ALL)
    private List<BranchEntity> branches;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "institute", cascade = CascadeType.ALL)
    private List<EmployeeEntity> employees;

}
