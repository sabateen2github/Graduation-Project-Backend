package gp.backend.controllers;

import gp.backend.dto.Branch;
import gp.backend.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchesController {

    private final BranchService branchService;

    @GetMapping
    public List<Branch> getAllBranches() {
        return branchService.getAll();
    }

    @GetMapping("/{id}")
    public Branch getBranch(@PathVariable String id) {
        if (StringUtils.isEmpty(id))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return branchService.getBranch(id);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGEMENT')")
    @PutMapping("/{id}")
    public void updateBranch(@PathVariable String id, @RequestBody Branch branch) {

        if (Stream.of(id, branch.getName(), branch.getId(), branch.getPhone(), branch.getInstituteId()).anyMatch(StringUtils::isEmpty))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (branch.getLocation() == null || !id.equals(branch.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        branchService.updateBranch((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), branch);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGEMENT')")
    @DeleteMapping("/{id}")
    public void deleteBranch(@PathVariable String id) {

        if (StringUtils.isEmpty(id))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        branchService.deleteBranch((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), id);
    }


    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGEMENT')")
    @PostMapping
    public Branch createBranch(@RequestBody Branch branch) {
        if (Stream.of(branch.getName(), branch.getPhone(), branch.getInstituteId()).anyMatch(StringUtils::isEmpty))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (branch.getLocation() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return branchService.createBranch((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), branch);
    }


}
