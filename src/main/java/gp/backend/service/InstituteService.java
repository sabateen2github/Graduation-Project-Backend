package gp.backend.service;

import gp.backend.data.InstitutesDAO;
import gp.backend.data.entities.InstituteEntity;
import gp.backend.dto.Institute;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstituteService {

    private final InstitutesDAO institutesDAO;

    private Institute mapFromEntity(InstituteEntity item) {
        Institute institute = new Institute();
        institute.setId(item.getId());
        institute.setEmail(item.getEmail());
        institute.setName(item.getName());
        institute.setPhone(item.getPhone());
        institute.setLogoUrl(item.getLogoUrl());
        return institute;
    }

    private InstituteEntity mapToEntity(Institute item) {
        InstituteEntity institute = new InstituteEntity();
        institute.setEmail(item.getEmail());
        institute.setName(item.getName());
        institute.setPhone(item.getPhone());
        institute.setLogoUrl(item.getLogoUrl());
        return institute;
    }

    public List<Institute> searchInstitutes(Optional<String> searchTerm) {
        if (searchTerm.isPresent()) {
            return institutesDAO.findByNameOrIdContaining(searchTerm.get(), searchTerm.get()).stream().map(this::mapFromEntity).collect(Collectors.toList());
        } else {
            return institutesDAO.findAll().stream().map(this::mapFromEntity).collect(Collectors.toList());
        }
    }


    public Institute getInstitute(String validatedId) {
        try {
            return institutesDAO.findById(validatedId).map(this::mapFromEntity).get();
        } catch (NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    public void deleteInstitute(String validatedId) {
        institutesDAO.deleteById(validatedId);
    }

    public void createInstitute(Institute validatedInstitute) {
        institutesDAO.save(mapToEntity(validatedInstitute));
    }

    public void saveInstitute(Institute validatedInstitute) {
        institutesDAO.save(mapToEntity(validatedInstitute));

    }


}
