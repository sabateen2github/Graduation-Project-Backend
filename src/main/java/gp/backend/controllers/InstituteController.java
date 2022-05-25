package gp.backend.controllers;

import gp.backend.dto.Institute;
import gp.backend.service.InstituteService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
@RequestMapping("/institute")
@RequiredArgsConstructor
public class InstituteController {

    private final InstituteService instituteService;

    @GetMapping
    public List<Institute> searchInstitutes(@RequestParam Optional<String> searchTerms) {
        return instituteService.searchInstitutes(searchTerms);
    }

    @GetMapping("/{id}")
    public Institute getInstitute(@PathVariable String id) {
        if (StringUtils.isEmpty(id))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return instituteService.getInstitute(id);
    }

    @DeleteMapping("/{id}")
    public void deleteInstitute(@PathVariable String id) {
        if (StringUtils.isEmpty(id))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        instituteService.deleteInstitute(id);
    }

    @PostMapping
    public void createInstitute(Institute institute) {
        if (Stream.of(institute.getEmail(), institute.getLogoUrl(), institute.getName(), institute.getPhone()).anyMatch(Objects::isNull))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        instituteService.createInstitute(institute);
    }

    @PutMapping("/{id}")
    public void updateInstitute(@PathVariable String id, @RequestBody Institute institute) {
        if (Stream.of(institute.getEmail(), institute.getLogoUrl(), institute.getName(), institute.getPhone()).anyMatch(Objects::isNull))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        instituteService.saveInstitute(institute);
    }
}
