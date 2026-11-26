package bg.nbu.banksystem.account.domain;

import bg.nbu.banksystem.starter.error.ErrorCategory;
import bg.nbu.banksystem.starter.error.ErrorCode;

/**
 * Stable error codes reported by Account Service.
 */
public enum AccountErrorCode implements ErrorCode {
    ACCOUNT_NOT_FOUND(ErrorCategory.NOT_FOUND, "Account was not found"),
    ACCOUNT_OWNER_NOT_FOUND(ErrorCategory.RULE_VIOLATION, "Account owner was not found"),
    ACCOUNT_OWNER_NOT_ACTIVE(ErrorCategory.RULE_VIOLATION, "Account owner must be active"),
    CUSTOMER_SERVICE_UNAVAILABLE(ErrorCategory.UNAVAILABLE, "Customer Service is temporarily unavailable"),
    INVALID_TRANSACTION_HISTORY_RANGE(ErrorCategory.VALIDATION, "Transaction history range is invalid"),
    TRANSACTION_HISTORY_RANGE_TOO_LARGE(ErrorCategory.VALIDATION,
        "Transaction history range must not exceed two years"),
    INVALID_TRANSACTION_HISTORY_PAGE(ErrorCategory.VALIDATION, "Transaction history page must be zero or greater"),
    INVALID_TRANSACTION_HISTORY_PAGE_SIZE(ErrorCategory.VALIDATION,
        "Transaction history page size must be between 1 and 100"),
    ACCOUNT_CLOSED(ErrorCategory.CONFLICT, "Account is closed and cannot be changed"),
    ACCOUNT_ALREADY_CLOSED(ErrorCategory.CONFLICT, "Account is already closed"),
    ACCOUNT_BALANCE_NOT_ZERO(ErrorCategory.RULE_VIOLATION, "Account balance must be zero before closure"),
    ACCOUNT_INSUFFICIENT_FUNDS(ErrorCategory.RULE_VIOLATION, "Account has insufficient funds"),
    ACCOUNT_CURRENCY_MISMATCH(ErrorCategory.VALIDATION, "Transaction currency does not match account currency"),
    ;

    private final ErrorCategory category;
    private final String title;

    AccountErrorCode(ErrorCategory category, String title) {
        this.category = category;
        this.title = title;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public ErrorCategory category() {
        return category;
    }

    @Override
    public String title() {
        return title;
    }
}
