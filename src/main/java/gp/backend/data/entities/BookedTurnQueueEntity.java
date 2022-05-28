package gp.backend.data.entities;

import gp.backend.dto.BookedTurnQueue;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
public class BookedTurnQueueEntity {

    @EmbeddedId
    private CompositeId id;

    @MapsId("queueId")
    @ManyToOne(optional = false)
    @JoinColumn
    private QueueEntity queue;

    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long turnId;
    private int position;
    private String logoUrl;
    private boolean physical;
    private BookedTurnQueue.QueueState state;


    @Embeddable
    @Data
    public static class CompositeId implements Serializable {
        private static final long serialVersionUID = 6854757354118584992L;
        private String uuid;
        private Long queueId;

    }

}
