package gp.backend.data;

import gp.backend.data.entities.QueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueueDAO extends JpaRepository<QueueEntity, String> {

    public List<QueueEntity> findAllByInstitute_IdAndBranch_Id(String instituteId, String branchId);

}
