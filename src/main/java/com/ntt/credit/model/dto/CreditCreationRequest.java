package com.ntt.credit.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class CreditCreationRequest {

  @NotBlank(message = "El customerId es obligatorio")
  private String customerId;

  @NotNull(message = "El limite de credito o prestamo es obligatorio")
  @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a 0")
  private BigDecimal creditLimit;

  private BigDecimal interestRate;

  // Para simplificar, la fecha de vencimiento inicial.
  private LocalDate dueDate;
}
