package com.ntt.credit.service.impl;

import static org.mockito.Mockito.verify;

import com.ntt.credit.config.CreditKafkaProperties;
import com.ntt.credit.model.kafka.DebtStatusCheckedEvent;
import com.ntt.credit.model.kafka.PaymentAppliedEvent;
import com.ntt.credit.model.kafka.PaymentRejectedEvent;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class CreditEventPublisherImplTest {

  @Mock private KafkaTemplate<String, Object> kafkaTemplate;

  private CreditEventPublisherImpl publisher;
  private CreditKafkaProperties properties;

  @BeforeEach
  void setUp() {
    properties = new CreditKafkaProperties();
    publisher = new CreditEventPublisherImpl(kafkaTemplate, properties);
  }

  @Test
  void publishDebtStatusCheckedSendsEventToConfiguredTopic() {
    DebtStatusCheckedEvent event =
        DebtStatusCheckedEvent.builder().customerId("customer-1").hasOverdueDebt(false).build();

    publisher.publishDebtStatusChecked(event);

    verify(kafkaTemplate).send(properties.getDebtStatusChecked(), "customer-1", event);
  }

  @Test
  void publishPaymentAppliedSendsEventToConfiguredTopic() {
    PaymentAppliedEvent event =
        PaymentAppliedEvent.builder().creditId("credit-1").amount(BigDecimal.TEN).build();

    publisher.publishPaymentApplied(event);

    verify(kafkaTemplate).send(properties.getPaymentApplied(), "credit-1", event);
  }

  @Test
  void publishPaymentRejectedSendsEventToConfiguredTopic() {
    PaymentRejectedEvent event =
        PaymentRejectedEvent.builder().creditId("credit-1").errorMessage("error").build();

    publisher.publishPaymentRejected(event);

    verify(kafkaTemplate).send(properties.getPaymentRejected(), "credit-1", event);
  }
}
