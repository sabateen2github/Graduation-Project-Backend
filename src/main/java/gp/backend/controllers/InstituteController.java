package gp.backend.controllers;

import gp.backend.dto.Institute;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/institute")
public class InstituteController {

    @GetMapping
    public List<Institute> searchInstitutes(@RequestParam Optional<String> searchTerms) {
        return new ArrayList<>();
    }

    @GetMapping("/{id}")
    public Institute getInstitute(@PathVariable String id) {

        return new Institute();
    }

    @DeleteMapping("/{id}")
    public void deleteInstitute(@PathVariable String id) {

    }

    @PostMapping
    public void createInstitute(Institute institute) {

    }
}
