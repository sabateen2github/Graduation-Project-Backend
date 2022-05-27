package gp.backend.service.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QueueLockKey {
    private String instituteId;
    private String branchId;
    private String queueId;
}
