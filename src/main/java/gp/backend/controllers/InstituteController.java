package gp.backend.controllers;

import gp.backend.dto.Institute;
import gp.backend.service.InstituteService;
import gp.backend.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
@RequestMapping("/institute")
@RequiredArgsConstructor
public class InstituteController {

    private final InstituteService instituteService;
    private final UploadService uploadService;


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

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteInstitute(@PathVariable String id) {
        if (StringUtils.isEmpty(id))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        instituteService.deleteInstitute(id);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public void createInstitute(@RequestPart Institute institute, @RequestPart Optional<MultipartFile> profilePic) {
        Optional<String> uploadUrl = handleInstitute(institute, profilePic);
        instituteService.createInstitute(institute, uploadUrl);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGEMENT')")
    @PutMapping()
    public void updateInstitute(@RequestPart Institute institute, @RequestPart Optional<MultipartFile> profilePic) {
        Optional<String> uploadUrl = handleInstitute(institute, profilePic);
        instituteService.saveInstitute((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), institute, uploadUrl);

    }

    private Optional<String> handleInstitute(Institute institute, Optional<MultipartFile> profilePic) {
        if (!validateInstitute(institute))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        Optional<String> uploadUrl = Optional.empty();
        if (profilePic.isPresent())
            uploadUrl = Optional.of(uploadService.upload(profilePic.get()));
        return uploadUrl;
    }

    private boolean validateInstitute(Institute institute) {
        return Stream.of(institute.getEmail(), institute.getName(), institute.getPhone()).anyMatch(StringUtils::isEmpty);
    }
}
