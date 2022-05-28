package gp.backend.controllers;

import gp.backend.dto.Institute;
import gp.backend.service.InstituteService;
import gp.backend.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteInstitute(@PathVariable String id) {
        if (StringUtils.isEmpty(id))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        instituteService.deleteInstitute(id);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public void createInstitute(@RequestPart @Parameter(schema = @Schema(type = "string", format = "binary")) Institute institute, @RequestPart Optional<MultipartFile> profilePic) {
        Optional<String> uploadUrl = handleInstitute(institute, profilePic);
        instituteService.createInstitute(institute, uploadUrl, false);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGEMENT')")
    @PutMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public void updateInstitute(@RequestPart @Parameter(schema = @Schema(type = "string", format = "binary")) Institute institute, @RequestPart Optional<MultipartFile> profilePic) {
        Optional<String> uploadUrl = handleInstitute(institute, profilePic);
        instituteService.saveInstitute((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), institute, uploadUrl);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/login/{id}")
    public String loginAsInstitute(@PathVariable String id) {
        if (StringUtils.isEmpty(id))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return instituteService.getJWTToken(id);
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
        return Stream.of(institute.getEmail(), institute.getName(), institute.getPhone()).noneMatch(StringUtils::isEmpty);
    }
}
