package com.ashuboi.service;

import com.ashuboi.Repo.TransactionRepo;
import com.ashuboi.TransactionStatusEnum;
import com.ashuboi.dto.TransactionRequest;
import com.ashuboi.dto.TransactionStatusDto;
import com.ashuboi.dto.TransferResponse;
import com.ashuboi.entity.TransactionEntity;
import com.ashuboi.exception.ResourceNotFoundException;
import com.ashuboi.payload.TransactionInitPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionService.class);
    private static final String TXN_INIT_TOPIC = "TXN-INIT";
    private static final String SCOPE_P2P = "P2P_TRANSFER";

    private final TransactionRepo transactionRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IdempotencyService idempotencyService;

    public TransactionService(
            TransactionRepo transactionRepo,
            KafkaTemplate<String, Object> kafkaTemplate,
            IdempotencyService idempotencyService) {
        this.transactionRepo = transactionRepo;
        this.kafkaTemplate = kafkaTemplate;
        this.idempotencyService = idempotencyService;
    }

    public TransferResponse doTransaction(Long fromUserId, TransactionRequest request, String idempotencyKey) {
        if (fromUserId.equals(request.getToUserId())) {
            throw new IllegalArgumentException("Cannot transfer to yourself");
        }

        return idempotencyService.execute(SCOPE_P2P, idempotencyKey, request, TransferResponse.class, () -> {
            BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
            TransactionEntity transaction = TransactionEntity.builder()
                    .fromUserId(fromUserId)
                    .toUserId(request.getToUserId())
                    .amount(amount)
                    .remark(request.getRemark())
                    .status(TransactionStatusEnum.PENDING)
                    .txnId(UUID.randomUUID().toString())
                    .idempotencyKey(idempotencyKey)
                    .build();
            transactionRepo.save(transaction);

            TransactionInitPayload payload = TransactionInitPayload.builder()
                    .id(transaction.getId())
                    .txnId(transaction.getTxnId())
                    .fromUserId(transaction.getFromUserId())
                    .toUserId(transaction.getToUserId())
                    .amount(transaction.getAmount())
                    .remark(transaction.getRemark())
                    .requestId(MDC.get("requestId"))
                    .build();

            kafkaTemplate.send(TXN_INIT_TOPIC, fromUserId.toString(), payload);
            LOGGER.info("Published TXN-INIT txnId={}", transaction.getTxnId());

            return TransferResponse.builder()
                    .txnId(transaction.getTxnId())
                    .status(TransactionStatusEnum.PENDING.name())
                    .idempotencyKey(idempotencyKey)
                    .build();
        });
    }

    public TransactionStatusDto getStatus(String txnId) {
        TransactionEntity transaction = transactionRepo.findByTxnId(txnId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + txnId));
        return toDto(transaction);
    }

    public List<TransactionStatusDto> history(Long userId) {
        return transactionRepo.findByFromUserIdOrToUserIdOrderByCreatedAtDesc(userId, userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private TransactionStatusDto toDto(TransactionEntity transaction) {
        return TransactionStatusDto.builder()
                .txnId(transaction.getTxnId())
                .status(transaction.getStatus().name())
                .reason(transaction.getReason())
                .fromUserId(transaction.getFromUserId())
                .toUserId(transaction.getToUserId())
                .amount(transaction.getAmount())
                .remark(transaction.getRemark())
                .build();
    }
}
