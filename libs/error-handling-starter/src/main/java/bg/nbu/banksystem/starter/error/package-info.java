/**
 * Public error-handling API for bank-system services.
 *
 * <p>
 * Services define their own {@link bg.nbu.banksystem.starter.error.ErrorCode} enums and
 * throw {@link bg.nbu.banksystem.starter.error.DomainException}. Nothing in this package
 * refers to HTTP.
 * </p>
 */
package bg.nbu.banksystem.starter.error;