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
        EmployeeEntity employeeEntity = employeeDAO.findById(Long.valueOf(validatedId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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

        InstituteEntity instituteEntity = institutesDAO.findById(Long.valueOf(instituteId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (validatedEmployee.getAccountType() == Employee.AccountType.ROLE_ADMIN && !instituteEntity.isAdmin())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        EmployeeEntity employeeEntity = new EmployeeEntity();
        employeeEntity.setInstitute(instituteEntity);

        if (validatedEmployee.getBranchId() != null) {
            BranchEntity branchEntity = branchDAO.findById(Long.valueOf(validatedEmployee.getBranchId())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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

        UserDataDTO userDataDTO = new UserDataDTO();
        if (validatedEmployee.getAccountType() == Employee.AccountType.ROLE_MANAGEMENT) {
            userDataDTO.setAppUserRoles(Arrays.asList(UserDataDTO.AppUserRolesEnum.MANAGEMENT));
        } else if (validatedEmployee.getAccountType() == Employee.AccountType.ROLE_HELP_DESK)
            userDataDTO.setAppUserRoles(Arrays.asList(UserDataDTO.AppUserRolesEnum.HELP_DESK));
        else if (validatedEmployee.getAccountType() == Employee.AccountType.ROLE_ADMIN)
            userDataDTO.setAppUserRoles(Arrays.asList(UserDataDTO.AppUserRolesEnum.ADMIN));
        else throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
        userDataDTO.setUsername(validatedEmployee.getUsername());
        userDataDTO.setPassword(validatedEmployee.getPassword().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST)));
        userDataDTO.setInstituteId(instituteId);

        authApi.signup(userDataDTO);
    }

    public void editEmployee(Employee validatedEmployee, String instituteId, Optional<String> profilePic) {
        EmployeeEntity employeeEntity = employeeDAO.findById(Long.valueOf(validatedEmployee.getId())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (validatedEmployee.getAccountType() == Employee.AccountType.ROLE_ADMIN && !employeeEntity.getInstitute().isAdmin())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        String oldUsername = employeeEntity.getUsername();

        if (validatedEmployee.getBranchId() != null) {
            BranchEntity branchEntity = branchDAO.findById(Long.valueOf(validatedEmployee.getBranchId())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            employeeEntity.setBranch(branchEntity);
        }

        employeeEntity.setUsername(validatedEmployee.getUsername());
        employeeEntity.setEmail(validatedEmployee.getEmail());
        employeeEntity.setAccountType(validatedEmployee.getAccountType());
        employeeEntity.setFullName(validatedEmployee.getFullName());
        employeeEntity.setName(validatedEmployee.getName());
        employeeEntity.setPhone(validatedEmployee.getPhone());
        employeeEntity.setDateOfBirth(validatedEmployee.getDateOfBirth());
        profilePic.ifPresent(employeeEntity::setProfilePic);

        employeeDAO.save(employeeEntity);

        UserDataDTO userDataDTO = new UserDataDTO();
        if (validatedEmployee.getAccountType() == Employee.AccountType.ROLE_MANAGEMENT) {
            userDataDTO.setAppUserRoles(Arrays.asList(UserDataDTO.AppUserRolesEnum.MANAGEMENT));
        } else if (validatedEmployee.getAccountType() == Employee.AccountType.ROLE_HELP_DESK)
            userDataDTO.setAppUserRoles(Arrays.asList(UserDataDTO.AppUserRolesEnum.HELP_DESK));
        else if (validatedEmployee.getAccountType() == Employee.AccountType.ROLE_ADMIN)
            userDataDTO.setAppUserRoles(Arrays.asList(UserDataDTO.AppUserRolesEnum.ADMIN));
        else throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
        userDataDTO.setUsername(oldUsername);
        userDataDTO.setPassword(validatedEmployee.getPassword().orElse(null));
        userDataDTO.setInstituteId(instituteId);
        userDataDTO.setNewUsername(validatedEmployee.getUsername());
        authApi.editUser(userDataDTO);
    }


    private Employee fillDTO(EmployeeEntity employeeEntity) {
        Employee employee = new Employee();
        employee.setEmail(employeeEntity.getEmail());
        employee.setAccountType(employeeEntity.getAccountType());
        employee.setPhone(employeeEntity.getPhone());
        employee.setFullName(employeeEntity.getFullName());
        employee.setName(employeeEntity.getName());
        employee.setDateOfBirth(employeeEntity.getDateOfBirth());
        BranchEntity branch = employeeEntity.getBranch();
        if (branch != null)
            employee.setBranchId(String.valueOf(branch.getId()));
        employee.setUsername(employeeEntity.getUsername());
        employee.setId(String.valueOf(employeeEntity.getId()));
        employee.setProfilePic(Optional.ofNullable(employeeEntity.getProfilePic()));
        return employee;
    }


    public List<Employee> findBySearchTerm(String instituteId, String searchTerm) {
        if (StringUtils.isEmpty(searchTerm))
            return employeeDAO.findByInstitute_Id(Long.valueOf(instituteId)).stream().map(this::fillDTO).collect(Collectors.toList());

        Long id = -1L;
        try {
            id = Long.valueOf(searchTerm);
        } catch (Exception e) {
            id = -1L;
        }

        return employeeDAO.findByInstitute_NameContainingIgnoreCaseAndNameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrId(searchTerm, searchTerm, searchTerm, id).stream().map(this::fillDTO).collect(Collectors.toList());
    }
}
