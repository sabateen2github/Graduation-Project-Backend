package gp.backend.data.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class QueueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private String id;

    private String name;
    @ManyToOne
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    private int queueSize;
    private int physicalSize;
    private int remoteSize;
    private int averageTime;
    private String currentTurnId;

}
