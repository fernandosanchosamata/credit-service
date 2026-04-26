package com.ntt.credit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.credit.model.dto.CreditCreationRequest;
import com.ntt.credit.model.dto.CreditResponse;
import com.ntt.credit.model.dto.TransactionRequest;
import com.ntt.credit.model.enums.CreditStatus;
import com.ntt.credit.model.enums.CreditType;
import com.ntt.credit.service.CreditService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CreditControllerTest {

  @Mock private CreditService creditService;

  @InjectMocks private CreditController controller;

  @Test
  void createCreditCardReturnsCreatedResponse() {
    CreditCreationRequest request = creditRequest();
    CreditResponse response = creditResponse();
    when(creditService.createCreditCard(request)).thenReturn(Single.just(response));

    var result = controller.createCreditCard(request).blockingGet();

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(result.getBody()).isEqualTo(response);
  }

  @Test
  void payDelegatesToServiceAndReturnsOkResponse() {
    TransactionRequest request = new TransactionRequest();
    request.setAmount(BigDecimal.valueOf(80));
    CreditResponse response = creditResponse();
    when(creditService.pay("credit-1", request)).thenReturn(Single.just(response));

    var result = controller.pay("credit-1", request).blockingGet();

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(response);
    verify(creditService).pay("credit-1", request);
  }

  @Test
  void getCreditsByCustomerIdReturnsFlowableResults() {
    CreditResponse response = creditResponse();
    when(creditService.getCreditsByCustomerId("customer-1")).thenReturn(Flowable.just(response));

    var result = controller.getCreditsByCustomerId("customer-1").toList().blockingGet();

    assertThat(result).containsExactly(response);
  }

  @Test
  void hasOverdueDebtReturnsBooleanResponse() {
    when(creditService.hasOverdueDebt("customer-1")).thenReturn(Single.just(false));

    var result = controller.hasOverdueDebt("customer-1").blockingGet();

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isFalse();
  }

  private CreditCreationRequest creditRequest() {
    CreditCreationRequest request = new CreditCreationRequest();
    request.setCustomerId("customer-1");
    request.setCreditLimit(BigDecimal.valueOf(1000));
    return request;
  }

  private CreditResponse creditResponse() {
    return CreditResponse.builder()
        .id("credit-1")
        .customerId("customer-1")
        .type(CreditType.CREDIT_CARD)
        .creditLimit(BigDecimal.valueOf(1000))
        .consumedAmount(BigDecimal.ZERO)
        .status(CreditStatus.ACTIVE)
        .currency("PEN")
        .build();
  }
}
