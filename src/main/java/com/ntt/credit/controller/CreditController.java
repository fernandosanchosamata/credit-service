package com.ntt.credit.controller;

import com.ntt.credit.model.dto.CreditCreationRequest;
import com.ntt.credit.model.dto.CreditResponse;
import com.ntt.credit.model.dto.TransactionRequest;
import com.ntt.credit.service.CreditService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CreditController {

  private final CreditService creditService;

  @PostMapping("/loan")
  @ResponseStatus(HttpStatus.CREATED)
  public Single<ResponseEntity<CreditResponse>> createLoan(
      @Valid @RequestBody CreditCreationRequest request) {
    log.info("Solicitud recibida para crear prestamo. customerId={}", request.getCustomerId());
    return creditService
        .createLoan(request)
        .doOnSuccess(response -> log.info("Prestamo creado. creditId={}", response.getId()))
        .doOnError(error -> log.warn("No se pudo crear prestamo: {}", error.getMessage()))
        .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @PostMapping("/credit-card")
  @ResponseStatus(HttpStatus.CREATED)
  public Single<ResponseEntity<CreditResponse>> createCreditCard(
      @Valid @RequestBody CreditCreationRequest request) {
    log.info(
        "Solicitud recibida para crear tarjeta de credito. customerId={}", request.getCustomerId());
    return creditService
        .createCreditCard(request)
        .doOnSuccess(
            response -> log.info("Tarjeta de credito creada. creditId={}", response.getId()))
        .doOnError(error -> log.warn("No se pudo crear tarjeta de credito: {}", error.getMessage()))
        .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @PostMapping("/{id}/consume")
  public Single<ResponseEntity<CreditResponse>> consume(
      @PathVariable String id, @Valid @RequestBody TransactionRequest request) {
    log.info("Solicitud recibida para consumo de credito. creditId={}", id);
    return creditService
        .consume(id, request)
        .doOnSuccess(response -> log.info("Consumo aplicado. creditId={}", response.getId()))
        .doOnError(error -> log.warn("No se pudo aplicar consumo: {}", error.getMessage()))
        .map(ResponseEntity::ok);
  }

  @PostMapping("/{id}/pay")
  public Single<ResponseEntity<CreditResponse>> pay(
      @PathVariable String id, @Valid @RequestBody TransactionRequest request) {
    log.info("Solicitud recibida para pago de credito. creditId={}", id);
    return creditService
        .pay(id, request)
        .doOnSuccess(response -> log.info("Pago aplicado. creditId={}", response.getId()))
        .doOnError(error -> log.warn("No se pudo aplicar pago: {}", error.getMessage()))
        .map(ResponseEntity::ok);
  }

  @GetMapping("/customer/{customerId}")
  public Flowable<CreditResponse> getCreditsByCustomerId(@PathVariable String customerId) {
    log.info("Solicitud recibida para consultar creditos. customerId={}", customerId);
    return creditService.getCreditsByCustomerId(customerId);
  }

  @GetMapping("/customer/{customerId}/has-overdue-debt")
  public Single<ResponseEntity<Boolean>> hasOverdueDebt(@PathVariable String customerId) {
    log.debug("Solicitud recibida para validar deuda vencida. customerId={}", customerId);
    return creditService.hasOverdueDebt(customerId).map(ResponseEntity::ok);
  }

  @GetMapping("/customer/{customerId}/has-active-credit-card")
  public Single<ResponseEntity<Boolean>> hasActiveCreditCard(@PathVariable String customerId) {
    log.debug("Solicitud recibida para validar tarjeta activa. customerId={}", customerId);
    return creditService.hasActiveCreditCard(customerId).map(ResponseEntity::ok);
  }
}
