package com.ntt.credit.repository;

import com.ntt.credit.model.entity.Credit;
import com.ntt.credit.model.enums.CreditStatus;
import com.ntt.credit.model.enums.CreditType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditRepository extends RxJava3CrudRepository<Credit, String> {

  Flowable<Credit> findByCustomerId(String customerId);

  Single<Long> countByCustomerIdAndType(String customerId, CreditType type);

  Single<Boolean> existsByCustomerIdAndTypeAndStatus(
      String customerId, CreditType type, CreditStatus status);

  Single<Boolean> existsByCustomerIdAndHasOverdueDebtTrue(String customerId);
}
