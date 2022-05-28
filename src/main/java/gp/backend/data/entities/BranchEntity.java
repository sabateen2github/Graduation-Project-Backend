package gp.backend.data.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
public class BranchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    private String name;
    private String phone;
    @ManyToOne(optional = false)
    @JoinColumn(name = "institute_id")
    private InstituteEntity institute;
    private double latitude;
    private double longitude;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "branch", cascade = CascadeType.ALL)
    private List<QueueEntity> queues;
}
