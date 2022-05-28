package gp.backend.data;

import gp.backend.data.entities.BookedTurnQueueEntity;
import gp.backend.dto.BookedTurnQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookedTurnQueueDAO extends JpaRepository<BookedTurnQueueEntity, BookedTurnQueueEntity.CompositeId> {

    public List<BookedTurnQueueEntity> findAllById_UuidAndState(String uuid, BookedTurnQueue.QueueState queueState);

    public List<BookedTurnQueueEntity> findAllById_UuidAndStateIsNot(String uuid, BookedTurnQueue.QueueState queueState);

    public Optional<BookedTurnQueueEntity> findByIdAndState(BookedTurnQueueEntity.CompositeId compositeId, BookedTurnQueue.QueueState queueState);


    public List<BookedTurnQueueEntity> findAllById_QueueIdAndState(Long queueId, BookedTurnQueue.QueueState queueState);

    public Optional<BookedTurnQueueEntity> findById_QueueIdAndTurnId(Long queueId, Long turnId);

    public Optional<BookedTurnQueueEntity> findById_QueueIdAndPosition(Long queueId, int position);
    public List<BookedTurnQueueEntity> findAllById_QueueIdAndPositionGreaterThan(Long queueId, int position);


}
