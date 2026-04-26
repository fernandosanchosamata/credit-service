package com.ntt.credit.service.impl;

import com.ntt.credit.client.CustomerClient;
import com.ntt.credit.model.dto.CreditCreationRequest;
import com.ntt.credit.model.dto.CreditResponse;
import com.ntt.credit.model.dto.CustomerSummaryResponse;
import com.ntt.credit.model.dto.TransactionRequest;
import com.ntt.credit.model.entity.Credit;
import com.ntt.credit.model.enums.CreditStatus;
import com.ntt.credit.model.enums.CreditType;
import com.ntt.credit.model.kafka.DebtStatusCheckedEvent;
import com.ntt.credit.model.kafka.PaymentAppliedEvent;
import com.ntt.credit.repository.CreditRepository;
import com.ntt.credit.service.CreditEventPublisher;
import com.ntt.credit.service.CreditService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Service;
import reactor.adapter.rxjava.RxJava3Adapter;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

  private static final String REDIS_DEBT_PREFIX = "DEUDA_CLIENTE_";

  private final CreditRepository creditRepository;
  private final CustomerClient customerClient;

  @Qualifier("stringRedisOperations")
  private final ReactiveRedisOperations<String, String> stringRedisOps;

  private final CreditEventPublisher creditEventPublisher;

  @Override
  public Single<CreditResponse> createLoan(CreditCreationRequest request) {
    return validateCustomerForNewProduct(request.getCustomerId())
        .flatMap(
            customer ->
                validateLoanRules(request, customer).flatMap(ignored -> persistLoan(request)));
  }

  @Override
  public Single<CreditResponse> createCreditCard(CreditCreationRequest request) {
    return validateCustomerForNewProduct(request.getCustomerId())
        .flatMap(customer -> persistCreditCard(request));
  }

  private Single<CustomerSummaryResponse> validateCustomerForNewProduct(String customerId) {
    return customerClient
        .getCustomerSummary(customerId)
        .flatMap(
            customer -> {
              if (!"ACTIVE".equals(customer.getStatus())) {
                return Single.error(
                    new IllegalArgumentException(
                        "No se pueden aperturar creditos para clientes inactivos."));
              }

              return hasOverdueDebt(customerId)
                  .flatMap(
                      hasDebt -> {
                        if (Boolean.TRUE.equals(hasDebt)) {
                          return Single.error(
                              new IllegalArgumentException(
                                  "El cliente mantiene deuda vencida y no puede adquirir nuevos productos."));
                        }
                        return Single.just(customer);
                      });
            });
  }

  private Single<Boolean> validateLoanRules(
      CreditCreationRequest request, CustomerSummaryResponse customer) {
    if (!"PERSONAL".equals(customer.getType())) {
      return Single.just(Boolean.TRUE);
    }

    return creditRepository
        .countByCustomerIdAndType(request.getCustomerId(), CreditType.LOAN)
        .flatMap(
            count -> {
              if (count > 0) {
                return Single.error(
                    new IllegalArgumentException(
                        "Un cliente personal solo puede tener un prestamo activo."));
              }
              return Single.just(Boolean.TRUE);
            });
  }

  private Single<CreditResponse> persistLoan(CreditCreationRequest request) {
    Credit credit =
        Credit.builder()
            .customerId(request.getCustomerId())
            .type(CreditType.LOAN)
            .creditLimit(request.getCreditLimit())
            .principalBalance(request.getCreditLimit())
            .interestRate(resolveInterestRate(request.getInterestRate()))
            .statementDate(LocalDate.now())
            .dueDate(resolveDueDate(request.getDueDate()))
            .status(CreditStatus.ACTIVE)
            .build();

    return saveAndSyncDebtState(credit);
  }

  private Single<CreditResponse> persistCreditCard(CreditCreationRequest request) {
    Credit credit =
        Credit.builder()
            .customerId(request.getCustomerId())
            .type(CreditType.CREDIT_CARD)
            .creditLimit(request.getCreditLimit())
            .consumedAmount(BigDecimal.ZERO)
            .interestRate(resolveInterestRate(request.getInterestRate()))
            .statementDate(LocalDate.now())
            .dueDate(resolveDueDate(request.getDueDate()))
            .status(CreditStatus.ACTIVE)
            .build();

    return saveAndSyncDebtState(credit);
  }

  private BigDecimal resolveInterestRate(BigDecimal interestRate) {
    return interestRate == null ? BigDecimal.ZERO : interestRate;
  }

  private LocalDate resolveDueDate(LocalDate dueDate) {
    return dueDate == null ? LocalDate.now().plusMonths(1) : dueDate;
  }

  @Override
  public Single<CreditResponse> consume(String creditId, TransactionRequest request) {
    return creditRepository
        .findById(creditId)
        .switchIfEmpty(Single.error(new IllegalArgumentException("Credito no encontrado.")))
        .flatMap(
            credit -> {
              if (credit.getStatus() != CreditStatus.ACTIVE) {
                return Single.error(new IllegalArgumentException("El credito no esta activo."));
              }

              if (credit.getType() != CreditType.CREDIT_CARD) {
                return Single.error(
                    new IllegalArgumentException("Solo las tarjetas de credito admiten consumos."));
              }

              BigDecimal newConsumedAmount = credit.getConsumedAmount().add(request.getAmount());
              if (newConsumedAmount.compareTo(credit.getCreditLimit()) > 0) {
                return Single.error(
                    new IllegalArgumentException(
                        "El consumo excede el limite disponible de la tarjeta."));
              }

              credit.setConsumedAmount(newConsumedAmount);
              return saveAndSyncDebtState(credit);
            });
  }

  @Override
  public Single<CreditResponse> pay(String creditId, TransactionRequest request) {
    return creditRepository
        .findById(creditId)
        .switchIfEmpty(Single.error(new IllegalArgumentException("Credito no encontrado.")))
        .flatMap(
            credit -> {
              BigDecimal outstandingBalance = getOutstandingBalance(credit);
              BigDecimal newOutstandingBalance = outstandingBalance.subtract(request.getAmount());

              if (newOutstandingBalance.compareTo(BigDecimal.ZERO) < 0) {
                return Single.error(
                    new IllegalArgumentException(
                        "El pago supera la deuda pendiente del producto."));
              }

              if (credit.getType() == CreditType.LOAN) {
                credit.setPrincipalBalance(newOutstandingBalance);
                if (newOutstandingBalance.compareTo(BigDecimal.ZERO) == 0) {
                  credit.setStatus(CreditStatus.CLOSED);
                }
              } else {
                credit.setConsumedAmount(newOutstandingBalance);
              }

              return saveAndSyncDebtState(credit)
                  .map(
                      response -> {
                        publishPaymentAppliedEvent(
                            credit, request.getAmount(), request.getExternalReference());
                        return response;
                      });
            });
  }

  @Override
  public Flowable<CreditResponse> getCreditsByCustomerId(String customerId) {
    return creditRepository.findByCustomerId(customerId).map(this::buildResponse);
  }

  @Override
  public Single<Boolean> hasOverdueDebt(String customerId) {
    return RxJava3Adapter.monoToMaybe(
            stringRedisOps.opsForValue().get(REDIS_DEBT_PREFIX + customerId))
        .map(value -> "TRUE".equals(value))
        .switchIfEmpty(Single.just(false))
        .map(
            hasDebt -> {
              publishDebtStatusCheckedEvent(customerId, hasDebt);
              return hasDebt;
            });
  }

  @Override
  public Single<Boolean> hasActiveCreditCard(String customerId) {
    return creditRepository.existsByCustomerIdAndTypeAndStatus(
        customerId, CreditType.CREDIT_CARD, CreditStatus.ACTIVE);
  }

  private Single<CreditResponse> saveAndSyncDebtState(Credit credit) {
    credit.setHasOverdueDebt(isOverdue(credit));

    return creditRepository.save(credit).flatMap(this::syncDebtKey).map(this::buildResponse);
  }

  private boolean isOverdue(Credit credit) {
    return credit.getDueDate() != null
        && credit.getDueDate().isBefore(LocalDate.now())
        && getOutstandingBalance(credit).compareTo(BigDecimal.ZERO) > 0;
  }

  private BigDecimal getOutstandingBalance(Credit credit) {
    if (credit.getType() == CreditType.LOAN) {
      return credit.getPrincipalBalance() == null ? BigDecimal.ZERO : credit.getPrincipalBalance();
    }

    return credit.getConsumedAmount() == null ? BigDecimal.ZERO : credit.getConsumedAmount();
  }

  private Single<Credit> syncDebtKey(Credit credit) {
    return creditRepository
        .existsByCustomerIdAndHasOverdueDebtTrue(credit.getCustomerId())
        .flatMap(
            hasOverdueDebt -> {
              if (Boolean.TRUE.equals(hasOverdueDebt)) {
                log.warn("Cliente {} marcado con deuda vencida en Redis.", credit.getCustomerId());
                return RxJava3Adapter.monoToSingle(
                        stringRedisOps
                            .opsForValue()
                            .set(REDIS_DEBT_PREFIX + credit.getCustomerId(), "TRUE"))
                    .map(ignored -> credit);
              }

              log.info(
                  "Cliente {} sin deuda vencida activa. Limpiando marca en Redis.",
                  credit.getCustomerId());
              return RxJava3Adapter.monoToSingle(
                      stringRedisOps
                          .opsForValue()
                          .delete(REDIS_DEBT_PREFIX + credit.getCustomerId()))
                  .map(ignored -> credit);
            });
  }

  private CreditResponse buildResponse(Credit credit) {
    return CreditResponse.builder()
        .id(credit.getId())
        .customerId(credit.getCustomerId())
        .type(credit.getType())
        .creditLimit(credit.getCreditLimit())
        .consumedAmount(credit.getConsumedAmount())
        .principalBalance(credit.getPrincipalBalance())
        .interestRate(credit.getInterestRate())
        .statementDate(credit.getStatementDate())
        .dueDate(credit.getDueDate())
        .hasOverdueDebt(credit.getHasOverdueDebt())
        .status(credit.getStatus())
        .currency(credit.getCurrency())
        .availableBalance(calculateAvailableBalance(credit))
        .build();
  }

  private BigDecimal calculateAvailableBalance(Credit credit) {
    if (credit.getType() != CreditType.CREDIT_CARD) {
      return BigDecimal.ZERO;
    }

    return credit
        .getCreditLimit()
        .subtract(
            credit.getConsumedAmount() == null ? BigDecimal.ZERO : credit.getConsumedAmount());
  }

  private void publishDebtStatusCheckedEvent(String customerId, Boolean hasDebt) {
    creditEventPublisher.publishDebtStatusChecked(
        DebtStatusCheckedEvent.builder()
            .customerId(customerId)
            .hasOverdueDebt(hasDebt)
            .checkedAt(LocalDateTime.now())
            .build());
  }

  private void publishPaymentAppliedEvent(
      Credit credit, BigDecimal amount, String externalReference) {
    creditEventPublisher.publishPaymentApplied(
        PaymentAppliedEvent.builder()
            .creditId(credit.getId())
            .customerId(credit.getCustomerId())
            .amount(amount)
            .outstandingBalance(getOutstandingBalance(credit))
            .status(credit.getStatus().name())
            .externalReference(externalReference)
            .appliedAt(LocalDateTime.now())
            .build());
  }
}
