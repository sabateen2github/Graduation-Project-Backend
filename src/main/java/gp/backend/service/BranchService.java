package gp.backend.service;

import gp.backend.data.BranchDAO;
import gp.backend.data.InstitutesDAO;
import gp.backend.data.entities.BranchEntity;
import gp.backend.data.entities.InstituteEntity;
import gp.backend.dto.Branch;
import gp.backend.dto.LatLng;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {
    private final BranchDAO branchDAO;
    private final InstitutesDAO institutesDAO;

    private Branch mapEntityToBranch(BranchEntity item) {
        Branch branch = new Branch();
        branch.setId(String.valueOf(item.getId()));
        LatLng latLng = new LatLng();
        latLng.setLat(item.getLatitude());
        latLng.setLng(item.getLongitude());
        branch.setLocation(latLng);
        branch.setName(item.getName());
        branch.setPhone(item.getPhone());
        branch.setPhone(item.getPhone());
        branch.setInstituteId(String.valueOf(item.getInstitute().getId()));
        branch.setWorkingDays(item.getWorkingDays().stream().map(it -> {
            Branch.WorkingDay workingDay = new Branch.WorkingDay();
            workingDay.setDay(it.getDay());
            workingDay.setHour(it.getHour());
            workingDay.setMinute(it.getMinute());
            workingDay.setPeriodInMinutes(it.getPeriodInMinutes());
            return workingDay;
        }).collect(Collectors.toSet()));
        return branch;
    }

    private BranchEntity mapEntityFromBranch(Branch item, BranchEntity branch) {

        branch.setLatitude(item.getLocation().getLat());
        branch.setLongitude(item.getLocation().getLng());
        if (item.getName() != null) branch.setName(item.getName());
        if (item.getPhone() != null) branch.setPhone(item.getPhone());
        if (item.getWorkingDays() != null)
            branch.setWorkingDays(item.getWorkingDays().stream().map(it -> {
                BranchEntity.WorkingDay workingDay = new BranchEntity.WorkingDay();
                workingDay.setDay(it.getDay());
                workingDay.setHour(it.getHour());
                workingDay.setMinute(it.getMinute());
                workingDay.setPeriodInMinutes(it.getPeriodInMinutes());
                return workingDay;
            }).collect(Collectors.toSet()));

        return branch;
    }

    public List<Branch> getAll() {
        return branchDAO.findAll().stream().map(this::mapEntityToBranch).collect(Collectors.toList());
    }

    public Branch getBranch(String validatedId) {
        try {
            return branchDAO.findById(Long.valueOf(validatedId)).map(this::mapEntityToBranch).get();
        } catch (NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    public void updateBranch(String instituteId, Branch validatedBranch) {

        BranchEntity branchEntity = branchDAO.findById(Long.valueOf(validatedBranch.getId())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!branchEntity.getInstitute().getId().equals(instituteId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        branchDAO.save(mapEntityFromBranch(validatedBranch, branchEntity));
    }

    public Branch createBranch(String instituteId, Branch validatedBranch) {

        InstituteEntity instituteEntity = institutesDAO.findById(Long.valueOf(validatedBranch.getInstituteId())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        InstituteEntity callerEntity = institutesDAO.findById(Long.valueOf(instituteId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!instituteEntity.getId().equals(callerEntity.getId()) && !callerEntity.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }


        BranchEntity.Day[] days = {BranchEntity.Day.Friday, BranchEntity.Day.Monday, BranchEntity.Day.Sunday, BranchEntity.Day.Saturday, BranchEntity.Day.Wednesday, BranchEntity.Day.Tuesday, BranchEntity.Day.Thursday};
        if (validatedBranch.getWorkingDays() == null) {
            Set<Branch.WorkingDay> workingDays = new HashSet<>();
            for (int i = 0; i < days.length; i++) {
                Branch.WorkingDay workingDay = new Branch.WorkingDay();
                workingDay.setDay(days[i]);
                workingDay.setHour(8);
                workingDay.setMinute(0);
                workingDay.setPeriodInMinutes(12 * 60);
                workingDays.add(workingDay);
            }
            validatedBranch.setWorkingDays(workingDays);
        }

        BranchEntity branch = new BranchEntity();
        branch.setInstitute(instituteEntity);
        return mapEntityToBranch(branchDAO.save(mapEntityFromBranch(validatedBranch, branch)));
    }

    public void deleteBranch(String instituteId, String id) {
        InstituteEntity callerEntity = institutesDAO.findById(Long.valueOf(instituteId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        BranchEntity branchEntity = branchDAO.findById(Long.valueOf(id)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!branchEntity.getInstitute().getId().equals(callerEntity.getId()) && !callerEntity.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        branchDAO.delete(branchEntity);
    }
}
