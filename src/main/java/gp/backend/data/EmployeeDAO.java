package gp.backend.data;

import gp.backend.data.entities.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeDAO extends JpaRepository<EmployeeEntity, Long> {

    public List<EmployeeEntity> findByInstitute_NameContainingIgnoreCaseAndNameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrId(String instituteName, String searchTermName, String searchTermFull, Long searchTermId);

    public List<EmployeeEntity> findByInstitute_Id(Long instituteId);

    public Optional<EmployeeEntity> findById(Long id);

    public Optional<EmployeeEntity> findByUsername(String username);

}
