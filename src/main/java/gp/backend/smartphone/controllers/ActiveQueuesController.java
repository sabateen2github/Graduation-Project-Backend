package gp.backend.smartphone.controllers;

import gp.backend.smartphone.api.ActiveQueuesApi;
import gp.backend.smartphone.model.Queue;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class ActiveQueuesController implements ActiveQueuesApi {

    @Override
    public ResponseEntity<List<Queue>> getActiveQueues(String id) {
        return ResponseEntity.ok(IntStream.range(1, 30).mapToObj(it -> {
            Queue queue = new Queue()
                    .duration(43 - it)
                    .state(Queue.StateEnum.ACTIVE)
                    .logo("https://financialallianceforwomen.org/wp-content/uploads/2015/07/BAE-Logo-600x600-profile-picture.jpg")
                    .name("Bank al Etihad " + it)
                    .queueSize(10)
                    .remoteQueueSize(5)
                    .position(3)
                    .physicalSize(5)
                    .averageTime(5)
                    .category("Withdraw/Deposit")
                    .turnId(556);
            return queue;
        }).collect(Collectors.toList()));
    }
}
