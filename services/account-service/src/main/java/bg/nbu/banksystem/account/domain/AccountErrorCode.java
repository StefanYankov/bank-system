package bg.nbu.banksystem.account.domain;

import bg.nbu.banksystem.starter.error.ErrorCategory;
import bg.nbu.banksystem.starter.error.ErrorCode;

/**
 * Stable error codes reported by Account Service.
 */
public enum AccountErrorCode implements ErrorCode {
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
