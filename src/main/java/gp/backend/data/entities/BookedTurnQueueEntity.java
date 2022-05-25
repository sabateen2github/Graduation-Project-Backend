package gp.backend.data.entities;

import gp.backend.dto.BookedTurnQueue;
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
    @ManyToOne
    @JoinColumn
    private QueueEntity queue;

    private int turnId;
    private int position;
    private String logoUrl;
    private BookedTurnQueue.QueueState state;


    @Embeddable
    @Getter
    @Setter
    public static class CompositeId implements Serializable {
        private static final long serialVersionUID = 6854757354118584992L;
        private String uuid;
        private String queueId;

    }

}
