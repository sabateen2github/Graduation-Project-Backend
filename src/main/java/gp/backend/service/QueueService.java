package gp.backend.service;

import gp.backend.data.BookedTurnQueueDAO;
import gp.backend.data.QueueDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueDAO queueDAO;
    private BookedTurnQueueDAO bookedTurnQueueDAO;

}
