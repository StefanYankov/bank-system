package bg.nbu.banksystem.customer.domain;

/**
 * Lifecycle status of the bank's relationship with a Customer.
 */
public enum CustomerStatus {

    /**
     * The relationship is active and the Customer record can be changed.
     */
    ACTIVE,

    /**
     * The relationship is closed. The record is retained, cannot be changed,
     * and can be reopened if the Customer returns.
     */
    CLOSED
}