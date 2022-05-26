package gp.backend.data;

import gp.backend.data.entities.BookedTurnQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookedTurnQueueDAO extends JpaRepository<BookedTurnQueueEntity, BookedTurnQueueEntity.CompositeId> {
}
