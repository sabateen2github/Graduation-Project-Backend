package gp.backend.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class UploadService {


    public String upload(MultipartFile multipartFile) {

        String filename = Base64.getEncoder().encodeToString((multipartFile.getOriginalFilename() + "-" + multipartFile.getName() + "-" + new Date()).getBytes(StandardCharsets.UTF_8)) + ".bin";
        File file = new File("./uploads/", filename);
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            IOUtils.copyLarge(multipartFile.getInputStream(), fileOutputStream);
        } catch (FileNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

        return "/files/" + filename;
    }
}
