package com.ntt.credit.listener;

import com.ntt.credit.config.CreditKafkaProperties;
import com.ntt.credit.model.dto.TransactionRequest;
import com.ntt.credit.model.kafka.ApplyPaymentCommand;
import com.ntt.credit.model.kafka.PaymentRejectedEvent;
import com.ntt.credit.service.CreditEventPublisher;
import com.ntt.credit.service.CreditService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditCommandListener {

  private final CreditService creditService;
  private final CreditKafkaProperties creditKafkaProperties;
  private final CreditEventPublisher creditEventPublisher;

  @KafkaListener(
      topics = "#{@creditKafkaProperties.applyPaymentCommand}",
      groupId = "${spring.kafka.consumer.group-id}")
  public void handleApplyPaymentCommand(ApplyPaymentCommand command) {
    if (command == null || command.getCreditId() == null || command.getAmount() == null) {
      log.warn("Se recibio un ApplyPaymentCommand invalido: {}", command);
      publishRejectedEvent(command, "ApplyPaymentCommand invalido");
      return;
    }

    TransactionRequest request = new TransactionRequest();
    request.setAmount(command.getAmount());
    request.setExternalReference(command.getExternalReference());

    creditService
        .pay(command.getCreditId(), request)
        .subscribe(
            response ->
                log.info(
                    "ApplyPaymentCommand procesado para credito {} en topic {}",
                    command.getCreditId(),
                    creditKafkaProperties.getApplyPaymentCommand()),
            error -> {
              log.error(
                  "Error procesando ApplyPaymentCommand para credito {}: {}",
                  command.getCreditId(),
                  error.getMessage());
              publishRejectedEvent(command, error.getMessage());
            });
  }

  private void publishRejectedEvent(ApplyPaymentCommand command, String errorMessage) {
    if (command == null || command.getCreditId() == null) {
      return;
    }

    creditEventPublisher.publishPaymentRejected(
        PaymentRejectedEvent.builder()
            .creditId(command.getCreditId())
            .externalReference(command.getExternalReference())
            .errorMessage(errorMessage)
            .rejectedAt(LocalDateTime.now())
            .build());
  }
}
