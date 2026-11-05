package bg.nbu.banksystem.account.domain;

import bg.nbu.banksystem.common.Identifiers;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import static bg.nbu.banksystem.account.domain.AccountConstraints.CURRENCY_LENGTH;
import static bg.nbu.banksystem.account.domain.AccountConstraints.MONEY_PRECISION;
import static bg.nbu.banksystem.account.domain.AccountConstraints.MONEY_SCALE;
import static bg.nbu.banksystem.account.domain.AccountConstraints.TRANSACTION_REFERENCE_MAX_LENGTH;
import static bg.nbu.banksystem.account.domain.AccountConstraints.TRANSACTION_TYPE_MAX_LENGTH;

/**
 * Immutable record of a successful account balance movement.
 */
@Getter
@Entity
@Table(name = "account_transactions")
public class AccountTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, updatable = false, length = TRANSACTION_TYPE_MAX_LENGTH)
    private TransactionType type;

    @Column(name = "amount", nullable = false, updatable = false, precision = MONEY_PRECISION, scale = MONEY_SCALE)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, updatable = false, length = CURRENCY_LENGTH)
    private Currency currency;

    @Column(name = "resulting_balance", nullable = false, updatable = false, precision = MONEY_PRECISION,
        scale = MONEY_SCALE)
    private BigDecimal resultingBalance;

    @Column(name = "reference", nullable = false, updatable = false, length = TRANSACTION_REFERENCE_MAX_LENGTH)
    private String reference;

    protected AccountTransaction() {
    }

    private AccountTransaction(Account account, TransactionType type, BigDecimal amount, Currency currency,
                               BigDecimal resultingBalance, String reference) {
        super(Identifiers.newUuid());
        this.account = Objects.requireNonNull(account, "account must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.amount = normalizePositiveAmount(amount);
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.resultingBalance = normalizeBalance(resultingBalance);
        this.reference = requireText(reference, "reference", TRANSACTION_REFERENCE_MAX_LENGTH);
    }

    static AccountTransaction record(Account account, TransactionType type, BigDecimal amount, Currency currency,
                                     BigDecimal resultingBalance, String reference) {
        return new AccountTransaction(account, type, amount, currency, resultingBalance, reference);
    }

    private static BigDecimal normalizePositiveAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        BigDecimal normalizedAmount;

        try {
            normalizedAmount = amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("amount must have at most " + MONEY_SCALE + " decimal places",
                exception);
        }

        if (normalizedAmount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        return normalizedAmount;
    }

    private static BigDecimal normalizeBalance(BigDecimal balance) {
        Objects.requireNonNull(balance, "resultingBalance must not be null");

        try {
            return balance.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("resultingBalance must have at most " + MONEY_SCALE + " decimal places",
                exception);
        }
    }

    private static String requireText(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalizedValue = value.trim();

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        if (normalizedValue.length() > maxLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
        }

        return normalizedValue;
    }
}
