package gp.backend.data;

import gp.backend.data.entities.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchDAO extends JpaRepository<BranchEntity, String> {
}
