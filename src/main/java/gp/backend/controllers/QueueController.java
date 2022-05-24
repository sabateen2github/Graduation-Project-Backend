package gp.backend.controllers;

import gp.backend.dto.BookedTurnQueue;
import gp.backend.dto.Queue;
import gp.backend.dto.QueueSpec;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/queues")
public class QueueController {


    @GetMapping("/active/{userId}")
    public List<BookedTurnQueue> getActiveQueues(@PathVariable String userId) {
        return new ArrayList<>();
    }

    @GetMapping("/archived/{userId}")
    public List<BookedTurnQueue> getArchivedQueues(@PathVariable String userId) {

        return new ArrayList<>();
    }

    @GetMapping("/queue")
    public Queue getQueue(@RequestParam String id, @RequestParam String branchId) {
        return new Queue();
    }


    @PutMapping("/queue/reset")
    public void resetQueue(@RequestParam String id, @RequestParam String branchId) {

    }

    @PutMapping("/queue/advance")
    public void advanceQueue(@RequestParam String id, @RequestParam String branchId) {

    }


    @PutMapping("/queue/book")
    public void bookQueue(@RequestParam String userId, @RequestParam String queueId, @RequestParam String branchId) {

    }

    @PutMapping("/queue")
    public void editQueueSpec(@RequestBody QueueSpec queueSpec) {

    }


    @DeleteMapping("/queue")
    public void deleteQueue(@RequestParam String id, @RequestParam String branchId) {

    }


    @GetMapping("/spec/all")
    public List<QueueSpec> getAllQueueSpecs(@RequestParam String branchId) {
        return new ArrayList<>();
    }

    @GetMapping("/queues/all")
    public List<Queue> getAllQueues(@RequestParam String branchId) {
        return new ArrayList<>();
    }

}
