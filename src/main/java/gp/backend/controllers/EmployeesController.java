package gp.backend.controllers;

import gp.backend.dto.Employee;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeesController {

    @GetMapping
    public List<Employee> searchEmployees(@RequestParam String searchTerm) {

        return new ArrayList<>();
    }


    @GetMapping("/{id}")
    public Employee getEmployee(@PathVariable String id) {
        return new Employee();
    }

    @PutMapping("/{id}")
    public void editEmployee(@PathVariable String id, @RequestBody Employee employee) {

    }

    @PostMapping
    public void createEmployee(Employee employee) {

    }


}
