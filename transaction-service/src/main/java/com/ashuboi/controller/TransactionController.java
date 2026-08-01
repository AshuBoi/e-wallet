package com.ashuboi.controller;

import com.ashuboi.dto.TransactionRequest;
import com.ashuboi.dto.TransactionStatusDto;
import com.ashuboi.dto.TransferResponse;
import com.ashuboi.security.AuthContext;
import com.ashuboi.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transaction-service")
public class TransactionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        Long fromUserId = AuthContext.requireUserId();
        LOGGER.info("Transfer from={} to={} amount={}", fromUserId, request.getToUserId(), request.getAmount());
        return ResponseEntity.ok(transactionService.doTransaction(fromUserId, request, idempotencyKey));
    }

    /** Backward-compatible alias. */
    @PostMapping("/doTransaction")
    public ResponseEntity<TransferResponse> doTransaction(
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return transfer(request, idempotencyKey);
    }

    @GetMapping("/status/{txnId}")
    public ResponseEntity<TransactionStatusDto> getStatus(@PathVariable String txnId) {
        return ResponseEntity.ok(transactionService.getStatus(txnId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<TransactionStatusDto>> history() {
        return ResponseEntity.ok(transactionService.history(AuthContext.requireUserId()));
    }
}
