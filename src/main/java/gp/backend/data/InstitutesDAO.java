package gp.backend.data;

import gp.backend.data.entities.InstituteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstitutesDAO extends JpaRepository<InstituteEntity, String> {

    public List<InstituteEntity> findByNameOrIdContaining(String searchTermName, String searchTermId);

}
