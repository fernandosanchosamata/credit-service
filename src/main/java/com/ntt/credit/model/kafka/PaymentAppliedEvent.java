package com.ntt.credit.model.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAppliedEvent {
  private String creditId;
  private String customerId;
  private BigDecimal amount;
  private BigDecimal outstandingBalance;
  private String status;
  private String externalReference;
  private LocalDateTime appliedAt;
}
