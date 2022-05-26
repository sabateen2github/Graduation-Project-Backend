package gp.backend.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileServingController {

    @GetMapping("/{filename}")
    public Resource getFile(@PathVariable String filename) {
        File file = new File("./uploads/" + filename);
        if (!file.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return new FileSystemResource(file);
    }

}
