package bg.nbu.banksystem.customer.domain;

import bg.nbu.banksystem.starter.error.ErrorCategory;
import bg.nbu.banksystem.starter.error.ErrorCode;

/**
 * Stable error codes reported by Customer Service.
 */
public enum CustomerErrorCode implements ErrorCode {

    CUSTOMER_ALREADY_CLOSED(ErrorCategory.CONFLICT, "Customer is already closed"), CUSTOMER_ALREADY_ACTIVE(
            ErrorCategory.CONFLICT, "Customer is already active"), CUSTOMER_CLOSED(ErrorCategory.CONFLICT,
            "Customer is closed and cannot be changed"), REPRESENTATIVE_NOT_FOUND(ErrorCategory.NOT_FOUND,
            "Representative not found"), REPRESENTATION_ALREADY_ENDED(ErrorCategory.CONFLICT,
            "Representation has already ended"), INVALID_REPRESENTATION_PERIOD(ErrorCategory.RULE_VIOLATION,
            "Representation end date precedes the start date");

    private final ErrorCategory category;
    private final String title;

    CustomerErrorCode(ErrorCategory category, String title) {
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