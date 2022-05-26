package gp.backend.data;

import gp.backend.data.entities.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeDAO extends JpaRepository<EmployeeEntity, String> {

    public List<EmployeeEntity> findByNameOrFullNameOrIdContaining(String searchTermName, String searchTermFull, String searchTermId);

}
