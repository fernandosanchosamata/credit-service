package com.ntt.credit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "credit.kafka.topics")
public class CreditKafkaProperties {
  private String applyPaymentCommand = "apply-payment-command-topic";
  private String paymentApplied = "payment-applied-topic";
  private String paymentRejected = "payment-rejected-topic";
  private String debtStatusChecked = "debt-status-checked-topic";
}
