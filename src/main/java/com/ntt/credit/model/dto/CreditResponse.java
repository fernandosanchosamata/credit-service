package com.ntt.credit.model.dto;

import com.ntt.credit.model.enums.CreditStatus;
import com.ntt.credit.model.enums.CreditType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreditResponse {
  private String id;
  private String customerId;
  private CreditType type;
  private BigDecimal creditLimit;
  private BigDecimal consumedAmount;
  private BigDecimal principalBalance;
  private BigDecimal interestRate;
  private LocalDate statementDate;
  private LocalDate dueDate;
  private Boolean hasOverdueDebt;
  private CreditStatus status;
  private String currency;

  private BigDecimal availableBalance; // Calculado.
}
