package bg.nbu.banksystem.account.domain;

import bg.nbu.banksystem.common.Identifiers;
import bg.nbu.banksystem.starter.error.DomainException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static bg.nbu.banksystem.account.domain.AccountConstraints.ACCOUNT_STATUS_MAX_LENGTH;
import static bg.nbu.banksystem.account.domain.AccountConstraints.ACCOUNT_TYPE_MAX_LENGTH;
import static bg.nbu.banksystem.account.domain.AccountConstraints.CURRENCY_LENGTH;
import static bg.nbu.banksystem.account.domain.AccountConstraints.IBAN_LENGTH;
import static bg.nbu.banksystem.account.domain.AccountConstraints.MONEY_PRECISION;
import static bg.nbu.banksystem.account.domain.AccountConstraints.MONEY_SCALE;

/**
 * Aggregate root for a Customer-owned bank account.
 */
@Getter
@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {

    private static final BigDecimal ZERO_BALANCE = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("createdAt ASC")
    private final List<AccountTransaction> transactions = new ArrayList<>();
    @Column(name = "iban", nullable = false, updatable = false, unique = true, length = IBAN_LENGTH)
    private String iban;
    @Column(name = "owner_customer_id", nullable = false, updatable = false)
    private UUID ownerCustomerId;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, updatable = false, length = ACCOUNT_TYPE_MAX_LENGTH)
    private AccountType type;
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, updatable = false, length = CURRENCY_LENGTH)
    private Currency currency;
    @Column(name = "balance", nullable = false, precision = MONEY_PRECISION, scale = MONEY_SCALE)
    private BigDecimal balance;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = ACCOUNT_STATUS_MAX_LENGTH)
    private AccountStatus status;

    protected Account() {
    }

    private Account(Iban iban, UUID ownerCustomerId, AccountType type, Currency currency) {
        super(Identifiers.newUuid());

        this.iban = Objects.requireNonNull(iban, "iban must not be null").value();
        this.ownerCustomerId = Objects.requireNonNull(ownerCustomerId, "ownerCustomerId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.balance = ZERO_BALANCE;
        this.status = AccountStatus.ACTIVE;
    }

    /**
     * Opens an active account with a zero balance.
     *
     * @param iban            externally visible account identifier
     * @param ownerCustomerId Customer Service identifier of the owner
     * @param type            account product type
     * @param currency        account currency
     * @return opened account
     */
    public static Account open(Iban iban, UUID ownerCustomerId, AccountType type, Currency currency) {
        return new Account(iban, ownerCustomerId, type, currency);
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

    /**
     * Credits a positive amount and records the resulting balance.
     *
     * @param amount              credit amount
     * @param transactionCurrency transaction currency
     * @param reference           client-safe transaction reference
     * @return immutable transaction entry
     */
    public AccountTransaction credit(BigDecimal amount, Currency transactionCurrency, String reference) {
        ensureActive();
        requireMatchingCurrency(transactionCurrency);
        BigDecimal normalizedAmount = normalizePositiveAmount(amount);
        BigDecimal newBalance = balance.add(normalizedAmount);
        AccountTransaction transaction = AccountTransaction.record(this, TransactionType.CREDIT, normalizedAmount,
            currency, newBalance, reference);

        balance = newBalance;
        transactions.add(transaction);
        return transaction;
    }

    /**
     * Debits a positive amount when sufficient funds are available.
     *
     * @param amount              debit amount
     * @param transactionCurrency transaction currency
     * @param reference           client-safe transaction reference
     * @return immutable transaction entry
     */
    public AccountTransaction debit(BigDecimal amount, Currency transactionCurrency, String reference) {
        ensureActive();
        requireMatchingCurrency(transactionCurrency);
        BigDecimal normalizedAmount = normalizePositiveAmount(amount);

        if (balance.compareTo(normalizedAmount) < 0) {
            throw new DomainException(AccountErrorCode.ACCOUNT_INSUFFICIENT_FUNDS);
        }

        BigDecimal newBalance = balance.subtract(normalizedAmount);
        AccountTransaction transaction = AccountTransaction.record(this, TransactionType.DEBIT, normalizedAmount,
            currency, newBalance, reference);

        balance = newBalance;
        transactions.add(transaction);
        return transaction;
    }

    /**
     * Permanently closes an active account with a zero balance.
     */
    public void close() {
        if (status == AccountStatus.CLOSED) {
            throw new DomainException(AccountErrorCode.ACCOUNT_ALREADY_CLOSED);
        }

        if (balance.compareTo(ZERO_BALANCE) != 0) {
            throw new DomainException(AccountErrorCode.ACCOUNT_BALANCE_NOT_ZERO);
        }

        status = AccountStatus.CLOSED;
    }

    /**
     * Returns an unmodifiable view of immutable transaction entries.
     *
     * @return transaction history in persistence order
     */
    public List<AccountTransaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    private void ensureActive() {
        if (status == AccountStatus.CLOSED) {
            throw new DomainException(AccountErrorCode.ACCOUNT_CLOSED);
        }
    }

    private void requireMatchingCurrency(Currency transactionCurrency) {
        Objects.requireNonNull(transactionCurrency, "transactionCurrency must not be null");

        if (transactionCurrency != currency) {
            throw new DomainException(AccountErrorCode.ACCOUNT_CURRENCY_MISMATCH);
        }
    }
}
