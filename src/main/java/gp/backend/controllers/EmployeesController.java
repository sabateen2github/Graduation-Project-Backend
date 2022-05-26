package gp.backend.controllers;

import gp.backend.dto.Employee;
import gp.backend.service.EmployeeService;
import gp.backend.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @GetMapping
    public List<Employee> searchEmployees(@RequestParam String searchTerm) {
        if (StringUtils.isEmpty(searchTerm)) return employeeService.getAll();
        else return employeeService.findBySearchTerm(searchTerm);
    }

    @GetMapping("/{id}")
    public Employee getEmployee(@PathVariable String id) {
        if (StringUtils.isEmpty(id))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return employeeService.getEmployee(id);
    }

    @PutMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public void editEmployee(@RequestPart Employee employee, @RequestPart Optional<MultipartFile> profilePic) {
        Optional<String> uploadUrl = handleEmployee(employee, profilePic);
        employeeService.editEmployee(employee, (String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), uploadUrl);

    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public void createEmployee(@RequestPart Employee employee, @RequestPart Optional<MultipartFile> profilePic) {
        Optional<String> uploadUrl = handleEmployee(employee, profilePic);
        employeeService.createEmployee(employee, (String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), uploadUrl);
    }


    private Optional<String> handleEmployee(Employee employee, Optional<MultipartFile> profilePic) {
        if (!validateEmployee(employee) || employee.getAccountType() == Employee.AccountType.ADMIN || !employee.getPassword().isPresent())
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
