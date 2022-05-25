package gp.backend.service;

import gp.backend.data.BranchDAO;
import gp.backend.data.entities.BranchEntity;
import gp.backend.dto.Branch;
import gp.backend.dto.LatLng;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {
    private final BranchDAO branchDAO;

    private Branch mapEntityToBranch(BranchEntity item) {
        Branch branch = new Branch();
        branch.setId(item.getId());
        LatLng latLng = new LatLng();
        latLng.setLat(item.getLatitude());
        latLng.setLng(item.getLongitude());
        branch.setLocation(latLng);
        branch.setName(item.getName());
        branch.setPhone(item.getPhone());
        branch.setPhone(item.getPhone());
        branch.setInstituteId(item.getInstitute().getId());
        return branch;
    }

    private BranchEntity mapEntityFromBranch(Branch item) {
        BranchEntity branch = new BranchEntity();
        branch.setId(item.getId());
        branch.setLatitude(item.getLocation().getLat());
        branch.setLongitude(item.getLocation().getLng());
        branch.setName(item.getName());
        branch.setPhone(item.getPhone());
        branch.setPhone(item.getPhone());
        return branch;
    }

    public List<Branch> getAll() {
        return branchDAO.findAll().stream().map(this::mapEntityToBranch).collect(Collectors.toList());
    }

    public Branch getBranch(String validatedId) {
        try {
            return branchDAO.findById(validatedId).map(this::mapEntityToBranch).get();
        } catch (NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    public void updateBranch(Branch validatedBranch) {
        branchDAO.save(mapEntityFromBranch(validatedBranch));
    }

    public void createBranch(Branch validatedBranch) {
        branchDAO.save(mapEntityFromBranch(validatedBranch));
    }
}
