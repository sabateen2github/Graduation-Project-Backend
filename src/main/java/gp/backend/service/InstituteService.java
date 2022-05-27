package gp.backend.service;

import gp.backend.data.InstitutesDAO;
import gp.backend.data.entities.InstituteEntity;
import gp.backend.dto.Institute;
import gp.backend.security.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;


    public List<Institute> searchInstitutes(Optional<String> searchTerm) {
        return searchTerm.map(s -> institutesDAO.findByNameOrIdContaining(s, s).stream().map(this::mapFromEntity).collect(Collectors.toList())).orElseGet(() -> institutesDAO.findAll().stream().map(this::mapFromEntity).collect(Collectors.toList()));
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

    public Institute createInstitute(Institute validatedInstitute, Optional<String> logoPic, boolean admin) {
        validatedInstitute.setLogoUrl(logoPic);
        return mapFromEntity(institutesDAO.save(mapToEntity(validatedInstitute, admin)));
    }

    public void saveInstitute(String instituteId, Institute validatedInstitute, Optional<String> logoPic) {
        InstituteEntity institute = institutesDAO.findById(instituteId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        institute.setEmail(validatedInstitute.getEmail());
        institute.setName(validatedInstitute.getName());
        institute.setPhone(validatedInstitute.getPhone());
        institute.setLogoUrl(logoPic.get());
        institutesDAO.save(institute);

    }

    private Institute mapFromEntity(InstituteEntity item) {
        Institute institute = new Institute();
        institute.setId(item.getId());
        institute.setEmail(item.getEmail());
        institute.setName(item.getName());
        institute.setPhone(item.getPhone());
        institute.setLogoUrl(Optional.ofNullable(item.getLogoUrl()));
        return institute;
    }

    private InstituteEntity mapToEntity(Institute item, boolean admin) {
        InstituteEntity institute = new InstituteEntity();
        institute.setEmail(item.getEmail());
        institute.setName(item.getName());
        institute.setPhone(item.getPhone());
        institute.setLogoUrl(item.getLogoUrl().orElse(null));
        institute.setAdmin(admin);
        return institute;
    }


    public String getJWTToken(String instituteId) {
        return jwtTokenProvider.generateAdminToken(instituteId);
    }
}
