package gp.backend.smartphone.controllers;

import gp.backend.smartphone.api.BranchesApi;
import gp.backend.smartphone.model.BookTurn;
import gp.backend.smartphone.model.QueueCategory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class BranchesController implements BranchesApi {

    @Override
    public ResponseEntity<List<QueueCategory>> getCategories(String id) {

        List<QueueCategory> queues = IntStream.range(0, 10).mapToObj(i -> {

            QueueCategory queueCategory = new QueueCategory();

            queueCategory.category("Category " + i)
                    .averageTime(20)
                    .duration(2100)
                    .queueSize(20)
                    .remoteQueueSize(10)
                    .physicalSize(10)
                    .name("Bank al Etihad");

            return queueCategory;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(queues);
    }

    @Override
    public ResponseEntity<Void> bookATurn(String id, BookTurn bookTurn) {
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
