package com.k44.stn.common.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/__test/errors")
public class TestErrorController {

    public record ValidationBody(@NotBlank String value) {}

    @PostMapping(value = "/validation", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void validation(@Valid @RequestBody ValidationBody body) {
        // no-op
    }

    @GetMapping("/not-found")
    public void notFound() {
        throw new NotFoundException(ErrorCode.SQUAD_NOT_FOUND, "Отряд не найден");
    }

    @GetMapping("/conflict")
    public void conflict() {
        throw new ConflictException(ErrorCode.USER_ALREADY_EXISTS, "Пользователь уже существует");
    }

    @GetMapping("/internal")
    public void internal() {
        throw new RuntimeException("boom");
    }
}