package com.ntt.credit.service.impl;

import com.ntt.credit.config.CreditKafkaProperties;
import com.ntt.credit.model.kafka.DebtStatusCheckedEvent;
import com.ntt.credit.model.kafka.PaymentAppliedEvent;
import com.ntt.credit.model.kafka.PaymentRejectedEvent;
import com.ntt.credit.service.CreditEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditEventPublisherImpl implements CreditEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final CreditKafkaProperties creditKafkaProperties;

  @Override
  public void publishDebtStatusChecked(DebtStatusCheckedEvent event) {
    kafkaTemplate.send(creditKafkaProperties.getDebtStatusChecked(), event.getCustomerId(), event);
    log.info("DebtStatusCheckedEvent publicado para cliente {}", event.getCustomerId());
  }

  @Override
  public void publishPaymentApplied(PaymentAppliedEvent event) {
    kafkaTemplate.send(creditKafkaProperties.getPaymentApplied(), event.getCreditId(), event);
    log.info("PaymentAppliedEvent publicado para credito {}", event.getCreditId());
  }

  @Override
  public void publishPaymentRejected(PaymentRejectedEvent event) {
    kafkaTemplate.send(creditKafkaProperties.getPaymentRejected(), event.getCreditId(), event);
    log.info("PaymentRejectedEvent publicado para credito {}", event.getCreditId());
  }
}
