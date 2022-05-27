package gp.backend.dto;

import lombok.Data;

@Data
public class BookedTurnQueue {
    private String turnId;
    private int position;
    private String logoUrl;
    private QueueState state;
    private Queue queue;

    public enum QueueState {
        ACTIVE, CANCELLED, COMPLETED
    }
}
