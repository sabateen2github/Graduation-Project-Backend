package gp.backend.controllers;

import gp.backend.dto.Branch;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/branches")
public class BranchesController {


    @GetMapping
    public List<Branch> getAllBranches() {
        return new ArrayList<>();
    }

    @GetMapping("/{id}")
    public Branch getBranch(@PathVariable String id) {
        return new Branch();
    }

    @PutMapping("/{id}")
    public void updateBranch(@PathVariable String id, @RequestBody Branch branch) {

    }

    @PostMapping
    public void createBranch(Branch branch) {

    }


}
