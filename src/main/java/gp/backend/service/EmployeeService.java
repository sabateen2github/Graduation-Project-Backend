package gp.backend.service;

import gp.backend.auth.handler.UserControllerApi;
import gp.backend.auth.model.UserDataDTO;
import gp.backend.data.BranchDAO;
import gp.backend.data.EmployeeDAO;
import gp.backend.data.InstitutesDAO;
import gp.backend.data.entities.BranchEntity;
import gp.backend.data.entities.EmployeeEntity;
import gp.backend.data.entities.InstituteEntity;
import gp.backend.dto.Employee;
import gp.backend.security.AppUserRole;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final UserControllerApi authApi;
    private final EmployeeDAO employeeDAO;
    private final BranchDAO branchDAO;
    private final InstitutesDAO institutesDAO;


    public Employee getEmployee(String instituteId, String validatedId) {
        EmployeeEntity employeeEntity = employeeDAO.findByInstitute_IdAndId(instituteId, validatedId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return fillDTO(employeeEntity);
    }

    public Employee getEmployeeByUsername(String instituteId, String username) {
        EmployeeEntity employeeEntity = employeeDAO.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean isAdmin = ((List<AppUserRole>) SecurityContextHolder.getContext().getAuthentication().getAuthorities()).contains(AppUserRole.ROLE_ADMIN);
        if (!isAdmin && !instituteId.equals(employeeEntity.getInstitute().getId()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return fillDTO(employeeEntity);
    }

    public void createEmployee(Employee validatedEmployee, String instituteId, Optional<String> profilePic) {
        if (employeeDAO.findByUsername(validatedEmployee.getUsername()).isPresent())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);

        InstituteEntity instituteEntity = institutesDAO.findById(instituteId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (validatedEmployee.getAccountType() == Employee.AccountType.ADMIN && !instituteEntity.isAdmin())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        EmployeeEntity employeeEntity = new EmployeeEntity();
        employeeEntity.setInstitute(instituteEntity);
        saveEmployee(validatedEmployee, instituteId, employeeEntity, profilePic);
    }

    public void editEmployee(Employee validatedEmployee, String instituteId, Optional<String> profilePic) {
        EmployeeEntity employeeEntity = employeeDAO.findByInstitute_IdAndId(instituteId, validatedEmployee.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (validatedEmployee.getAccountType() == Employee.AccountType.ADMIN && !employeeEntity.getInstitute().isAdmin())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        saveEmployee(validatedEmployee, instituteId, employeeEntity, profilePic);
    }


    private UserDataDTO getUserDataDTO(Employee validatedEmployee, String instituteId) {
        UserDataDTO userDataDTO = new UserDataDTO();
        if (validatedEmployee.getAccountType() == Employee.AccountType.MANAGEMENT) {
            userDataDTO.setAppUserRoles(Arrays.asList(UserDataDTO.AppUserRolesEnum.MANAGEMENT));
        } else if (validatedEmployee.getAccountType() == Employee.AccountType.HELP_DESK)
            userDataDTO.setAppUserRoles(Arrays.asList(UserDataDTO.AppUserRolesEnum.HELP_DESK));
        else if (validatedEmployee.getAccountType() == Employee.AccountType.ADMIN)
            userDataDTO.setAppUserRoles(Arrays.asList(UserDataDTO.AppUserRolesEnum.ADMIN));
        else throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
        userDataDTO.setUsername(validatedEmployee.getUsername());
        userDataDTO.setPassword(validatedEmployee.getPassword().get());
        userDataDTO.setInstituteId(instituteId);
        return userDataDTO;
    }

    private void saveEmployee(Employee validatedEmployee, String instituteId, EmployeeEntity employeeEntity, Optional<String> profilePic) {
        UserDataDTO userDataDTO = getUserDataDTO(validatedEmployee, instituteId);

        if (validatedEmployee.getBranchId() != null) {
            BranchEntity branchEntity = branchDAO.findById(validatedEmployee.getBranchId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            employeeEntity.setBranch(branchEntity);
        }

        employeeEntity.setEmail(validatedEmployee.getEmail());
        employeeEntity.setAccountType(validatedEmployee.getAccountType());
        employeeEntity.setFullName(validatedEmployee.getFullName());
        employeeEntity.setName(validatedEmployee.getName());
        employeeEntity.setUsername(validatedEmployee.getUsername());
        employeeEntity.setPhone(validatedEmployee.getPhone());
        employeeEntity.setDateOfBirth(validatedEmployee.getDateOfBirth());
        profilePic.ifPresent(employeeEntity::setProfilePic);

        employeeDAO.save(employeeEntity);
        authApi.signup(userDataDTO);
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
