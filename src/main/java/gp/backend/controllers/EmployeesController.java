package gp.backend.controllers;

import gp.backend.dto.Employee;
import gp.backend.service.EmployeeService;
import gp.backend.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeesController {

    private final EmployeeService employeeService;
    private final UploadService uploadService;

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGEMENT')")
    @GetMapping
    public List<Employee> searchEmployees(@RequestParam String searchTerm) {
        return employeeService.findBySearchTerm((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), searchTerm);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGEMENT') or hasRole('ROLE_HELP_DESK')")
    @GetMapping("/{id}")
    public Employee getEmployee(@PathVariable String id) {
        if (StringUtils.isEmpty(id))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return employeeService.getEmployee((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), id);
    }


    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGEMENT') or hasRole('ROLE_HELP_DESK')")
    @GetMapping("/username")
    public Employee getEmployeeByUsername(@RequestParam String username) {
        if (StringUtils.isEmpty(username))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        Employee employee = employeeService.getEmployeeByUsername((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), username);
        return employee;
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGEMENT')")
    @PutMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public void editEmployee(@RequestPart Employee employee, @RequestPart Optional<MultipartFile> profilePic) {
        Optional<String> uploadUrl = handleEmployee(employee, profilePic);
        employeeService.editEmployee(employee, (String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), uploadUrl);

    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGEMENT')")
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public void createEmployee(@RequestPart Employee employee, @RequestPart Optional<MultipartFile> profilePic) {
        Optional<String> uploadUrl = handleEmployee(employee, profilePic);
        employeeService.createEmployee(employee, (String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), uploadUrl);
    }


    private Optional<String> handleEmployee(Employee employee, Optional<MultipartFile> profilePic) {
        if (!validateEmployee(employee) || employee.getAccountType() == Employee.AccountType.ROLE_ADMIN || !employee.getPassword().isPresent())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        Optional<String> uploadUrl = Optional.empty();
        if (profilePic.isPresent())
            uploadUrl = Optional.of(uploadService.upload(profilePic.get()));
        return uploadUrl;
    }


    private boolean validateEmployee(Employee employee) {
        return Stream.of(employee, employee.getAccountType(), employee.getName(), employee.getPhone(), employee.getPassword(), employee.getBranchId(), employee.getDateOfBirth(), employee.getEmail(), employee.getUsername(), employee.getFullName()).anyMatch(Objects::isNull);
    }

}
