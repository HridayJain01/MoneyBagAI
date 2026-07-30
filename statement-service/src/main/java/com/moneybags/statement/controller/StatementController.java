package com.moneybags.statement.controller;
import com.moneybags.statement.dto.StatementResponse;
import com.moneybags.statement.service.StatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
@RestController @RequestMapping("/api/v1/statements") @RequiredArgsConstructor
public class StatementController {
    private final StatementService service;
    @GetMapping("/{accountNo}")
    StatementResponse generate(
            @PathVariable String accountNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.generate(accountNo, from, to);
    }
}
