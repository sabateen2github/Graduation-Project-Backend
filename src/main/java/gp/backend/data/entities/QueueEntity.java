package gp.backend.data.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
public class QueueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private String id;

    private String name;
    @ManyToOne(optional = false)
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    @ManyToOne(optional = false)
    @JoinColumn(name = "institute_id")
    private InstituteEntity institute;

    private int queueSize;
    private int physicalSize;
    private int remoteSize;
    private int averageTime;
    private String currentTurnId;

    private Date start;
    private int count;

}
