package gp.backend.service;

import gp.backend.auth.handler.UserControllerApi;
import gp.backend.auth.model.UserDataDTO;
import gp.backend.data.EmployeeDAO;
import gp.backend.data.entities.BranchEntity;
import gp.backend.data.entities.EmployeeEntity;
import gp.backend.dto.Employee;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final UserControllerApi authApi;
    private final EmployeeDAO employeeDAO;

    private final EntityManager entityManager;


    @Transactional
    public void createEmployee(Employee validatedEmployee, String instituteId, Optional<String> profilePic) {
        if (employeeDAO.findByInstitute_IdAndId(instituteId, validatedEmployee.getId()).isPresent()) {
            handleSave(validatedEmployee, instituteId, profilePic);
        } else throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private void handleSave(Employee validatedEmployee, String instituteId, Optional<String> profilePic) {
        UserDataDTO userDataDTO = new UserDataDTO();

        if (validatedEmployee.getAccountType() == Employee.AccountType.MANAGEMENT) {
            userDataDTO.setAppUserRoles(Arrays.asList(UserDataDTO.AppUserRolesEnum.MANAGEMENT));
        } else if (validatedEmployee.getAccountType() == Employee.AccountType.HELP_DESK)
            userDataDTO.setAppUserRoles(Arrays.asList(UserDataDTO.AppUserRolesEnum.HELP_DESK));
        else throw new RuntimeException("Invalid account type!");

        userDataDTO.setUsername(validatedEmployee.getUsername());
        userDataDTO.setPassword(validatedEmployee.getPassword().get());
        userDataDTO.setInstituteId(instituteId);

        if (!profilePic.isPresent())
            profilePic = Optional.of("./content/default-profile-pic.png");

        EmployeeEntity employeeEntity = fillEntity(validatedEmployee, profilePic);
        employeeDAO.save(employeeEntity);

        authApi.signup(userDataDTO);
    }


    public Employee getEmployee(String instituteId, String validatedId) {
        EmployeeEntity employeeEntity = employeeDAO.findByInstitute_IdAndId(instituteId, validatedId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return fillDTO(employeeEntity);
    }

    public void editEmployee(Employee validatedEmployee, String instituteId, Optional<String> profilePic) {
        if (employeeDAO.findByInstitute_IdAndId(instituteId, validatedEmployee.getId()).isPresent()) {
            handleSave(validatedEmployee, instituteId, profilePic);
        } else throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private EmployeeEntity fillEntity(Employee validatedEmployee, Optional<String> profilePic) {
        BranchEntity branchEntity = entityManager.getReference(BranchEntity.class, validatedEmployee.getBranchId());
        EmployeeEntity employeeEntity = new EmployeeEntity();
        employeeEntity.setBranch(branchEntity);
        employeeEntity.setEmail(validatedEmployee.getEmail());
        employeeEntity.setAccountType(validatedEmployee.getAccountType());
        employeeEntity.setFullName(validatedEmployee.getFullName());
        employeeEntity.setName(validatedEmployee.getName());
        employeeEntity.setUsername(validatedEmployee.getUsername());
        employeeEntity.setPhone(validatedEmployee.getPhone());
        employeeEntity.setDateOfBirth(validatedEmployee.getDateOfBirth());
        if (profilePic.isPresent())
            employeeEntity.setProfilePic(profilePic.get());
        return employeeEntity;
    }

    private Employee fillDTO(EmployeeEntity employeeEntity) {
        Employee employee = new Employee();
        employee.setEmail(employeeEntity.getEmail());
        employee.setAccountType(employeeEntity.getAccountType());
        employee.setPhone(employeeEntity.getPhone());
        employee.setFullName(employeeEntity.getFullName());
        employee.setName(employeeEntity.getName());
        employee.setDateOfBirth(employeeEntity.getDateOfBirth());
        employee.setBranchId(employeeEntity.getBranch().getId());
        employee.setUsername(employeeEntity.getUsername());
        employee.setId(employeeEntity.getId());
        employee.setProfilePic(Optional.ofNullable(employeeEntity.getProfilePic()));
        return employee;
    }


    public List<Employee> findBySearchTerm(String instituteId, String searchTerm) {
        if (StringUtils.isEmpty(searchTerm))
            return employeeDAO.findByInstitute_Id(instituteId).stream().map(this::fillDTO).collect(Collectors.toList());

        return employeeDAO.findByInstitute_IdAndNameOrFullNameOrIdContaining(instituteId, searchTerm, searchTerm, searchTerm).stream().map(this::fillDTO).collect(Collectors.toList());
    }
}
