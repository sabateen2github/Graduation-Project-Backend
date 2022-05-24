package gp.backend.dto;

import lombok.Data;


@Data
public class Queue {

    private QueueSpec queueSpec;
    private int queueSize;
    private int physicalSize;
    private int remoteSize;
    private int averageTime;
    private String currentTurnId;

}
