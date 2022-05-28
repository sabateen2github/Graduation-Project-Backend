package gp.backend.data;

import gp.backend.data.entities.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchDAO extends JpaRepository<BranchEntity, Long> {
}
