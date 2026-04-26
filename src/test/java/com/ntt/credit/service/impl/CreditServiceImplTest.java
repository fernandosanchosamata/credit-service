package com.ntt.credit.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.credit.client.CustomerClient;
import com.ntt.credit.model.dto.CreditCreationRequest;
import com.ntt.credit.model.dto.CustomerSummaryResponse;
import com.ntt.credit.model.dto.TransactionRequest;
import com.ntt.credit.model.entity.Credit;
import com.ntt.credit.model.kafka.PaymentAppliedEvent;
import com.ntt.credit.model.enums.CreditStatus;
import com.ntt.credit.model.enums.CreditType;
import com.ntt.credit.repository.CreditRepository;
import com.ntt.credit.service.CreditEventPublisher;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CreditServiceImplTest {

  @Mock private CreditRepository creditRepository;
  @Mock private CustomerClient customerClient;
  @Mock private ReactiveRedisOperations<String, String> stringRedisOps;
  @Mock private ReactiveValueOperations<String, String> valueOperations;
  @Mock private CreditEventPublisher creditEventPublisher;

  private CreditServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new CreditServiceImpl(
            creditRepository, customerClient, stringRedisOps, creditEventPublisher);
  }

  @Test
  void createLoanPersistsLoanWhenCustomerIsEligible() {
    CreditCreationRequest request = creditRequest();
    when(customerClient.getCustomerSummary("customer-1")).thenReturn(Single.just(activeCustomer()));
    when(stringRedisOps.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("DEUDA_CLIENTE_customer-1")).thenReturn(Mono.empty());
    when(creditRepository.countByCustomerIdAndType("customer-1", CreditType.LOAN))
        .thenReturn(Single.just(0L));
    when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> saveCredit(invocation.getArgument(0)));
    when(creditRepository.existsByCustomerIdAndHasOverdueDebtTrue("customer-1"))
        .thenReturn(Single.just(false));
    when(valueOperations.delete("DEUDA_CLIENTE_customer-1")).thenReturn(Mono.just(true));

    var response = service.createLoan(request).blockingGet();

    assertThat(response.getId()).isEqualTo("credit-1");
    assertThat(response.getType()).isEqualTo(CreditType.LOAN);
    assertThat(response.getPrincipalBalance()).isEqualByComparingTo("1000");
    verify(creditEventPublisher).publishDebtStatusChecked(any());
  }

  @Test
  void createLoanRejectsCustomerWithOverdueDebt() {
    CreditCreationRequest request = creditRequest();
    when(customerClient.getCustomerSummary("customer-1")).thenReturn(Single.just(activeCustomer()));
    when(stringRedisOps.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("DEUDA_CLIENTE_customer-1")).thenReturn(Mono.just("TRUE"));

    var observer = service.createLoan(request).test();

    observer.assertError(error -> error.getMessage().contains("deuda vencida"));
    verify(creditRepository, never()).save(any());
  }

  @Test
  void consumeUpdatesCreditCardBalance() {
    Credit card =
        Credit.builder()
            .id("credit-1")
            .customerId("customer-1")
            .type(CreditType.CREDIT_CARD)
            .creditLimit(BigDecimal.valueOf(1000))
            .consumedAmount(BigDecimal.valueOf(100))
            .dueDate(LocalDate.now().plusDays(5))
            .status(CreditStatus.ACTIVE)
            .build();
    TransactionRequest request = transactionRequest(BigDecimal.valueOf(50));
    when(creditRepository.findById("credit-1")).thenReturn(Maybe.just(card));
    when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
    when(creditRepository.existsByCustomerIdAndHasOverdueDebtTrue("customer-1"))
        .thenReturn(Single.just(false));
    when(stringRedisOps.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.delete("DEUDA_CLIENTE_customer-1")).thenReturn(Mono.just(true));

    var response = service.consume("credit-1", request).blockingGet();

    assertThat(response.getConsumedAmount()).isEqualByComparingTo("150");
    assertThat(response.getAvailableBalance()).isEqualByComparingTo("850");
  }

  @Test
  void payLoanClosesCreditAndPublishesPaymentApplied() {
    Credit loan =
        Credit.builder()
            .id("credit-1")
            .customerId("customer-1")
            .type(CreditType.LOAN)
            .creditLimit(BigDecimal.valueOf(1000))
            .principalBalance(BigDecimal.valueOf(100))
            .dueDate(LocalDate.now().plusDays(5))
            .status(CreditStatus.ACTIVE)
            .build();
    TransactionRequest request = transactionRequest(BigDecimal.valueOf(100));
    request.setExternalReference("tx-1");
    when(creditRepository.findById("credit-1")).thenReturn(Maybe.just(loan));
    when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
    when(creditRepository.existsByCustomerIdAndHasOverdueDebtTrue("customer-1"))
        .thenReturn(Single.just(false));
    when(stringRedisOps.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.delete("DEUDA_CLIENTE_customer-1")).thenReturn(Mono.just(true));

    var response = service.pay("credit-1", request).blockingGet();

    assertThat(response.getStatus()).isEqualTo(CreditStatus.CLOSED);
    assertThat(response.getPrincipalBalance()).isEqualByComparingTo("0");
    verify(creditEventPublisher).publishPaymentApplied(any(PaymentAppliedEvent.class));
  }

  private CreditCreationRequest creditRequest() {
    CreditCreationRequest request = new CreditCreationRequest();
    request.setCustomerId("customer-1");
    request.setCreditLimit(BigDecimal.valueOf(1000));
    request.setInterestRate(BigDecimal.valueOf(12));
    request.setDueDate(LocalDate.now().plusMonths(1));
    return request;
  }

  private TransactionRequest transactionRequest(BigDecimal amount) {
    TransactionRequest request = new TransactionRequest();
    request.setAmount(amount);
    return request;
  }

  private CustomerSummaryResponse activeCustomer() {
    CustomerSummaryResponse response = new CustomerSummaryResponse();
    response.setId("customer-1");
    response.setType("PERSONAL");
    response.setProfile("REGULAR");
    response.setStatus("ACTIVE");
    response.setDocumentNumber("12345678");
    return response;
  }

  private Single<Credit> saveCredit(Credit credit) {
    credit.setId("credit-1");
    return Single.just(credit);
  }
}
