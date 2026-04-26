package com.ntt.credit.model.entity;

import com.ntt.credit.model.enums.CreditStatus;
import com.ntt.credit.model.enums.CreditType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "credits")
public class Credit {

  @Id private String id;

  @Indexed private String customerId; // Referencia Hexadecimal ObjectId del Customer

  private CreditType type;

  private BigDecimal creditLimit;

  @Builder.Default private BigDecimal consumedAmount = BigDecimal.ZERO;

  @Builder.Default private BigDecimal principalBalance = BigDecimal.ZERO;

  private BigDecimal interestRate;

  private LocalDate statementDate;

  private LocalDate dueDate;

  @Builder.Default private Boolean hasOverdueDebt = false;

  @Builder.Default private CreditStatus status = CreditStatus.ACTIVE;

  @Builder.Default private String currency = "PEN";
}
