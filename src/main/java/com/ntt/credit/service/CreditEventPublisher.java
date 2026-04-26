package com.ntt.credit.service;

import com.ntt.credit.model.kafka.DebtStatusCheckedEvent;
import com.ntt.credit.model.kafka.PaymentAppliedEvent;
import com.ntt.credit.model.kafka.PaymentRejectedEvent;

public interface CreditEventPublisher {

  void publishDebtStatusChecked(DebtStatusCheckedEvent event);

  void publishPaymentApplied(PaymentAppliedEvent event);

  void publishPaymentRejected(PaymentRejectedEvent event);
}
