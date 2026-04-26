package com.ntt.credit.model.kafka;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRejectedEvent {
  private String creditId;
  private String externalReference;
  private String errorMessage;
  private LocalDateTime rejectedAt;
}
