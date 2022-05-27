package gp.backend;

import gp.backend.dto.Employee;
import gp.backend.dto.Institute;
import gp.backend.service.EmployeeService;
import gp.backend.service.InstituteService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class Initializer implements CommandLineRunner {

    private final InstituteService instituteService;
    private final EmployeeService employeeService;

    @Override
    public void run(String... args) throws Exception {

        Institute institute = new Institute();
        institute.setPhone("0000");
        institute.setName("admin");
        institute.setEmail("adminEmail");
        institute = instituteService.createInstitute(institute, Optional.empty(), true);

        Employee employee = new Employee();
        employee.setAccountType(Employee.AccountType.ROLE_ADMIN);
        employee.setUsername("admin");
        employee.setName("Alaa Sabateen");
        employee.setEmail("adminEmail");
        employee.setPhone("07788999");
        employee.setFullName("Alaa Khaled Mohammad Al-Sabateen");
        employee.setPassword(Optional.of("admin"));
        employeeService.createEmployee(employee, institute.getId(), Optional.empty());
    }
}
