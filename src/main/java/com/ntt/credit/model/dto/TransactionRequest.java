package com.ntt.credit.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TransactionRequest {

  @NotNull(message = "El monto de la transaccion es obligatorio")
  @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a 0")
  private BigDecimal amount;

  private String externalReference;
}
