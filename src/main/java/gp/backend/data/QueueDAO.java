package gp.backend.data;

import gp.backend.data.entities.QueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueDAO extends JpaRepository<QueueEntity, String> {

}
