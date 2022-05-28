package gp.backend;

import gp.backend.data.InstitutesDAO;
import gp.backend.dto.Employee;
import gp.backend.dto.Institute;
import gp.backend.service.EmployeeService;
import gp.backend.service.InstituteService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class Initializer implements CommandLineRunner {

    private final InstituteService instituteService;
    private final InstitutesDAO institutesDAO;
    private final EmployeeService employeeService;

    @Value("${security.jwt.token.username}")
    private String username;

    @Override
    public void run(String... args) throws Exception {

        if (institutesDAO.findByAdmin(true).isPresent()) return;

        try {
            Institute institute = new Institute();
            institute.setPhone("0000");
            institute.setName("admin");
            institute.setEmail("adminEmail");
            institute = instituteService.createInstitute(institute, Optional.empty(), true);

            Employee employee = new Employee();
            employee.setAccountType(Employee.AccountType.ROLE_ADMIN);
            employee.setUsername(username);
            employee.setName("Alaa Sabateen");
            employee.setEmail("adminEmail");
            employee.setPhone("07788999");
            employee.setFullName("Alaa Khaled Mohammad Al-Sabateen");
            employee.setPassword(Optional.of("alaa"));
            employeeService.createEmployee(employee, institute.getId(), Optional.empty());
        } catch (ResponseStatusException e) {
            if (e.getStatus() == HttpStatus.UNPROCESSABLE_ENTITY)
                e.printStackTrace();
            else throw e;
        }

    }
}
