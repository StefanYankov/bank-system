package bg.nbu.banksystem.account.application;

import bg.nbu.banksystem.account.application.dto.AccountSummaryDto;
import bg.nbu.banksystem.account.application.dto.AccountTransactionViewDto;
import bg.nbu.banksystem.account.application.dto.AccountViewDto;
import bg.nbu.banksystem.account.domain.Account;
import bg.nbu.banksystem.account.domain.AccountTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Maps Account domain entities to application views.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AccountMapper {

    /**
     * Converts an Account to its detailed view.
     *
     * @param account the Account
     * @return detailed Account view
     */
    AccountViewDto toView(Account account);

    /**
     * Converts an Account to its list representation.
     *
     * @param account the Account
     * @return Account summary
     */
    AccountSummaryDto toSummary(Account account);

    /**
     * Converts an immutable transaction entity to its application view.
     *
     * @param transaction the Account transaction
     * @return transaction view
     */
    AccountTransactionViewDto toTransactionView(AccountTransaction transaction);
}
