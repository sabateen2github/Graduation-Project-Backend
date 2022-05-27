package gp.backend.data;

import gp.backend.data.entities.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeDAO extends JpaRepository<EmployeeEntity, String> {

    public List<EmployeeEntity> findByInstitute_IdAndNameOrFullNameOrIdContaining(String instituteId, String searchTermName, String searchTermFull, String searchTermId);

    public List<EmployeeEntity> findByInstitute_Id(String instituteId);

    public Optional<EmployeeEntity> findByInstitute_IdAndId(String instituteId, String id);

    public Optional<EmployeeEntity> findByUsername(String username);

}
