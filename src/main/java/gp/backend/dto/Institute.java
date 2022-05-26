package gp.backend.dto;

import lombok.Data;

import java.util.Optional;

@Data
public class Institute {
    private String name;
    private String id;
    private Optional<String> logoUrl;
    private String email;
    private String phone;
}
