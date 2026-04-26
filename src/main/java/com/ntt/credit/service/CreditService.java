package com.ntt.credit.service;

import com.ntt.credit.model.dto.CreditCreationRequest;
import com.ntt.credit.model.dto.CreditResponse;
import com.ntt.credit.model.dto.TransactionRequest;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public interface CreditService {

  Single<CreditResponse> createLoan(CreditCreationRequest request);

  Single<CreditResponse> createCreditCard(CreditCreationRequest request);

  Single<CreditResponse> consume(String creditId, TransactionRequest request);

  Single<CreditResponse> pay(String creditId, TransactionRequest request);

  Flowable<CreditResponse> getCreditsByCustomerId(String customerId);

  Single<Boolean> hasOverdueDebt(String customerId);

  Single<Boolean> hasActiveCreditCard(String customerId);
}
