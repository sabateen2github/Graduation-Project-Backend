package gp.backend.data.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class BranchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private String id;

    private String name;
    private String phone;
    @ManyToOne
    @JoinColumn(name = "institute_id")
    private InstituteEntity institute;
    private double latitude;
    private double longitude;
}
