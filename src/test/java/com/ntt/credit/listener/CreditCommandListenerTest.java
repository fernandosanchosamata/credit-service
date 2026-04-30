package com.ntt.credit.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.credit.config.CreditKafkaProperties;
import com.ntt.credit.model.dto.CreditResponse;
import com.ntt.credit.model.dto.TransactionRequest;
import com.ntt.credit.model.kafka.ApplyPaymentCommand;
import com.ntt.credit.model.kafka.PaymentRejectedEvent;
import com.ntt.credit.service.CreditEventPublisher;
import com.ntt.credit.service.CreditService;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditCommandListenerTest {

  @Mock private CreditService creditService;
  @Mock private CreditEventPublisher creditEventPublisher;

  private CreditCommandListener listener;

  @BeforeEach
  void setUp() {
    listener =
        new CreditCommandListener(creditService, new CreditKafkaProperties(), creditEventPublisher);
  }

  @Test
  void handleApplyPaymentCommandPaysCreditWhenCommandIsValid() {
    ApplyPaymentCommand command =
        ApplyPaymentCommand.builder()
            .creditId("credit-1")
            .amount(BigDecimal.valueOf(25))
            .externalReference("tx-1")
            .build();
    when(creditService.pay(any(), any())).thenReturn(Single.just(CreditResponse.builder().build()));

    listener.handleApplyPaymentCommand(command);

    ArgumentCaptor<TransactionRequest> requestCaptor =
        ArgumentCaptor.forClass(TransactionRequest.class);
    verify(creditService).pay(org.mockito.Mockito.eq("credit-1"), requestCaptor.capture());
    verify(creditEventPublisher, never()).publishPaymentRejected(any());
    org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().getAmount())
        .isEqualByComparingTo("25");
    org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().getExternalReference())
        .isEqualTo("tx-1");
  }

  @Test
  void handleApplyPaymentCommandPublishesRejectedEventWhenServiceFails() {
    ApplyPaymentCommand command =
        ApplyPaymentCommand.builder()
            .creditId("credit-1")
            .amount(BigDecimal.valueOf(25))
            .externalReference("tx-1")
            .build();
    when(creditService.pay(any(), any()))
        .thenReturn(Single.error(new IllegalArgumentException("Saldo insuficiente")));

    listener.handleApplyPaymentCommand(command);

    ArgumentCaptor<PaymentRejectedEvent> eventCaptor =
        ArgumentCaptor.forClass(PaymentRejectedEvent.class);
    verify(creditEventPublisher).publishPaymentRejected(eventCaptor.capture());
    org.assertj.core.api.Assertions.assertThat(eventCaptor.getValue().getCreditId())
        .isEqualTo("credit-1");
    org.assertj.core.api.Assertions.assertThat(eventCaptor.getValue().getExternalReference())
        .isEqualTo("tx-1");
    org.assertj.core.api.Assertions.assertThat(eventCaptor.getValue().getErrorMessage())
        .isEqualTo("Saldo insuficiente");
  }

  @Test
  void handleApplyPaymentCommandPublishesRejectedEventWhenCommandIsInvalid() {
    ApplyPaymentCommand command =
        ApplyPaymentCommand.builder().creditId("credit-1").externalReference("tx-1").build();

    listener.handleApplyPaymentCommand(command);

    verify(creditService, never()).pay(any(), any());
    verify(creditEventPublisher).publishPaymentRejected(any(PaymentRejectedEvent.class));
  }

  @Test
  void handleApplyPaymentCommandIgnoresNullCommand() {
    listener.handleApplyPaymentCommand(null);

    verify(creditService, never()).pay(any(), any());
    verify(creditEventPublisher, never()).publishPaymentRejected(any());
  }
}
