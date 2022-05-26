package gp.backend.data;

import gp.backend.data.entities.QueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueueDAO extends JpaRepository<QueueEntity, String> {

}
