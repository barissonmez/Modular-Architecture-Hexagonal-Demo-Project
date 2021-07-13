package com.hexagonaldemo.paymentapi;

import com.hexagonaldemo.paymentapi.account.port.LockPort;
import com.hexagonaldemo.paymentapi.adapters.BalanceFakeAdapter;
import com.hexagonaldemo.paymentapi.adapters.PaymentFakeAdapter;
import com.hexagonaldemo.paymentapi.balance.BalanceRetrieveUseCaseHandler;
import com.hexagonaldemo.paymentapi.balance.BalanceTransactionCreateUseCaseHandler;
import com.hexagonaldemo.paymentapi.balance.model.Balance;
import com.hexagonaldemo.paymentapi.balance.service.BalanceValidator;
import com.hexagonaldemo.paymentapi.common.exception.PaymentApiBusinessException;
import com.hexagonaldemo.paymentapi.payment.PaymentCreateUseCaseHandler;
import com.hexagonaldemo.paymentapi.payment.model.Payment;
import com.hexagonaldemo.paymentapi.payment.model.PaymentState;
import com.hexagonaldemo.paymentapi.payment.usecase.PaymentCreate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PaymentCreateTest {

    @Test
    void should_create_payment_when_balance_is_sufficient() {
        doPayment(1L, "10.00", "10.00", "0.00");
        doPayment(1L, "4.50", "5.50", "1.00");
    }

    @Test
    void should_not_create_payment_when_balance_is_not_sufficient() {
        failPayment(1L, "10.00", "5.00");
        failPayment(1L, "10.00", "0.00");
    }

    private void doPayment(Long accountId, String price, String balanceAmountBeforePayment, String balanceAmountAfterPayment) {
        // given
        Balance balanceBeforePayment = Balance.builder()
                .id(1L)
                .accountId(accountId)
                .amount(new BigDecimal(balanceAmountBeforePayment))
                .build();

        Balance balanceAfterPayment = Balance.builder()
                .id(1L)
                .accountId(accountId)
                .amount(new BigDecimal(balanceAmountAfterPayment))
                .build();

        Payment expectedPayment = Payment.builder()
                .id(1L)
                .accountId(accountId)
                .accountId(accountId)
                .price(new BigDecimal(price))
                .referenceCode("ref1")
                .state(PaymentState.SUCCESS)
                .build();

        // and
        BalanceFakeAdapter balanceAdapter = new BalanceFakeAdapter(balanceBeforePayment, balanceAfterPayment);
        var balanceTransactionCreateUseCaseHandler = new BalanceTransactionCreateUseCaseHandler(balanceAdapter);
        var balanceRetrieveUseCaseHandler = new BalanceRetrieveUseCaseHandler(balanceAdapter);
        PaymentCreateUseCaseHandler paymentCreateUseCaseHandler = new PaymentCreateUseCaseHandler(
                retrieveFakeAccountLockPort(),
                new PaymentFakeAdapter(expectedPayment),
                new BalanceValidator());

        //when
        PaymentCreate paymentCreate = PaymentCreate.builder()
                .accountId(accountId)
                .price(new BigDecimal(price))
                .referenceCode("ref1")
                .build();

        Payment payment = paymentCreateUseCaseHandler.handle(paymentCreate);

        //then
        assertThat(payment).isNotNull().isEqualTo(Payment.builder()
                .id(1L)
                .accountId(accountId)
                .price(new BigDecimal(price))
                .referenceCode("ref1")
                .state(PaymentState.SUCCESS)
                .build());
    }

    private void failPayment(Long accountId, String price, String balanceAmountBeforePayment) {
        // given
        Balance balanceBeforePayment = Balance.builder()
                .id(1L)
                .accountId(accountId)
                .amount(new BigDecimal(balanceAmountBeforePayment))
                .build();

        Payment expectedPayment = Payment.builder()
                .id(1L)
                .accountId(accountId)
                .accountId(accountId)
                .price(new BigDecimal(price))
                .referenceCode("ref1")
                .state(PaymentState.SUCCESS)
                .build();

        // and
        BalanceTransactionCreateUseCaseHandler balanceTransactionCreateUseCase = new BalanceTransactionCreateUseCaseHandler(new BalanceFakeAdapter(balanceBeforePayment, balanceBeforePayment));
        PaymentCreateUseCaseHandler paymentCreateUseCase = new PaymentCreateUseCaseHandler(
                retrieveFakeAccountLockPort(),
                new PaymentFakeAdapter(expectedPayment),
                new BalanceValidator());

        //when
        PaymentCreate paymentCreate = PaymentCreate.builder()
                .accountId(accountId)
                .price(new BigDecimal(price))
                .referenceCode("ref1")
                .build();

        Throwable throwable = catchThrowable(() -> paymentCreateUseCase.handle(paymentCreate));

        //then
        assertThat(throwable).isNotNull().isInstanceOf(PaymentApiBusinessException.class);
        assertThat(((PaymentApiBusinessException) throwable).getKey()).isEqualTo("paymentapi.balance.notSufficient");
    }

    private LockPort retrieveFakeAccountLockPort() {
        return new LockPort() {
            @Override
            public void lock(Long accountId) {
            }

            @Override
            public void unlock(Long accountId) {
            }
        };
    }

}
