package gp.backend.controllers;

import gp.backend.dto.BookedTurnQueue;
import gp.backend.dto.LatLng;
import gp.backend.dto.Queue;
import gp.backend.dto.QueueSpec;
import gp.backend.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/queues")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @GetMapping("/active/{userId}")
    public List<BookedTurnQueue> getActiveQueues(@PathVariable String userId) {
        if (StringUtils.isEmpty(userId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return queueService.getActiveQueues(userId);
    }

    @GetMapping("/archived/{userId}")
    public List<BookedTurnQueue> getArchivedQueues(@PathVariable String userId) {
        if (StringUtils.isEmpty(userId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return queueService.getArchivedQueues(userId);
    }

    @GetMapping("/queue")
    public Queue getQueue(@RequestParam String id, @RequestParam String branchId) {
        if (StringUtils.isEmpty(id) || StringUtils.isEmpty(branchId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return queueService.getQueue((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), branchId, id);
    }


    @PutMapping("/queue/reset")
    public void resetQueue(@RequestParam String id, @RequestParam String branchId) {
        if (StringUtils.isEmpty(id) || StringUtils.isEmpty(branchId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        queueService.resetQueue((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), branchId, id);
    }

    @PutMapping("/queue/advance")
    public void advanceQueue(@RequestParam String id, @RequestParam String branchId) {
        if (StringUtils.isEmpty(id) || StringUtils.isEmpty(branchId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        queueService.advanceQueue((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), branchId, id);
    }


    @PutMapping("/queue/book")
    public void bookQueue(@RequestParam String userId, @RequestParam String queueId, @RequestParam String branchId, @RequestParam LatLng location) {
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(branchId) || StringUtils.isEmpty(queueId) || location == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        queueService.bookQueue(userId, branchId, queueId, location);
    }

    @PutMapping("/queue/book/toggle")
    public void switchUserLocationMode(@RequestParam String userId, @RequestParam String queueId, @RequestParam String branchId) {
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(branchId) || StringUtils.isEmpty(queueId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        queueService.toggleQueueMode((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), userId, branchId, queueId);
    }

    @DeleteMapping("/queue/book")
    public void cancelTurn(@RequestParam String userId, @RequestParam String queueId, @RequestParam String branchId) {
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(branchId) || StringUtils.isEmpty(queueId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        queueService.cancelTurn(userId, branchId, queueId);

    }

    @PutMapping("/queue")
    public void editQueueSpec(@RequestBody QueueSpec queueSpec) {
        if (queueSpec == null || StringUtils.isEmpty(queueSpec.getName()) || StringUtils.isEmpty(queueSpec.getId()) || StringUtils.isEmpty(queueSpec.getBranchId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        queueService.editQueueSpec((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), queueSpec);
    }

    @DeleteMapping("/queue")
    public void deleteQueue(@RequestParam String id, @RequestParam String branchId) {
        if (StringUtils.isEmpty(id) || StringUtils.isEmpty(branchId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        queueService.deleteQueue((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), branchId, id);
    }


    @GetMapping("/spec/all")
    public List<QueueSpec> getAllQueueSpecs(@RequestParam String branchId) {
        if (StringUtils.isEmpty(branchId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return queueService.getAllQueueSpecs((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), branchId);
    }

    @GetMapping("/queues/all")
    public List<Queue> getAllQueues(@RequestParam String branchId) {
        if (StringUtils.isEmpty(branchId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return queueService.getAllQueues((String) SecurityContextHolder.getContext().getAuthentication().getCredentials(), branchId);
    }

}
