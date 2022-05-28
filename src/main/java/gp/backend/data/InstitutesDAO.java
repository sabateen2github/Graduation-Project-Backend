package gp.backend.data;

import gp.backend.data.entities.InstituteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstitutesDAO extends JpaRepository<InstituteEntity, Long> {

    public List<InstituteEntity> findByNameContainingIgnoreCaseOrId(String searchTermName, Long searchTermId);

    public Optional<InstituteEntity> findByAdmin(boolean admin);
}
