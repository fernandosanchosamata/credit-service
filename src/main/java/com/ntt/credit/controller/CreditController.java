package com.ntt.credit.controller;

import com.ntt.credit.model.dto.CreditCreationRequest;
import com.ntt.credit.model.dto.CreditResponse;
import com.ntt.credit.model.dto.TransactionRequest;
import com.ntt.credit.service.CreditService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
public class CreditController {

  private final CreditService creditService;

  @PostMapping("/loan")
  @ResponseStatus(HttpStatus.CREATED)
  public Single<ResponseEntity<CreditResponse>> createLoan(
      @Valid @RequestBody CreditCreationRequest request) {
    return creditService
        .createLoan(request)
        .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @PostMapping("/credit-card")
  @ResponseStatus(HttpStatus.CREATED)
  public Single<ResponseEntity<CreditResponse>> createCreditCard(
      @Valid @RequestBody CreditCreationRequest request) {
    return creditService
        .createCreditCard(request)
        .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @PostMapping("/{id}/consume")
  public Single<ResponseEntity<CreditResponse>> consume(
      @PathVariable String id, @Valid @RequestBody TransactionRequest request) {
    return creditService.consume(id, request).map(ResponseEntity::ok);
  }

  @PostMapping("/{id}/pay")
  public Single<ResponseEntity<CreditResponse>> pay(
      @PathVariable String id, @Valid @RequestBody TransactionRequest request) {
    return creditService.pay(id, request).map(ResponseEntity::ok);
  }

  @GetMapping("/customer/{customerId}")
  public Flowable<CreditResponse> getCreditsByCustomerId(@PathVariable String customerId) {
    return creditService.getCreditsByCustomerId(customerId);
  }

  @GetMapping("/customer/{customerId}/has-overdue-debt")
  public Single<ResponseEntity<Boolean>> hasOverdueDebt(@PathVariable String customerId) {
    return creditService.hasOverdueDebt(customerId).map(ResponseEntity::ok);
  }

  @GetMapping("/customer/{customerId}/has-active-credit-card")
  public Single<ResponseEntity<Boolean>> hasActiveCreditCard(@PathVariable String customerId) {
    return creditService.hasActiveCreditCard(customerId).map(ResponseEntity::ok);
  }
}
