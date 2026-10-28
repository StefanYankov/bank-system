package bg.nbu.banksystem.customer.application;

import bg.nbu.banksystem.customer.application.dto.AddressDto;
import bg.nbu.banksystem.customer.application.dto.AddressViewDto;
import bg.nbu.banksystem.customer.application.dto.CustomerSummaryDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerViewDto;
import bg.nbu.banksystem.customer.application.dto.LegalEntityCustomerViewDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeViewDto;
import bg.nbu.banksystem.customer.domain.Address;
import bg.nbu.banksystem.customer.domain.Customer;
import bg.nbu.banksystem.customer.domain.IndividualCustomer;
import bg.nbu.banksystem.customer.domain.LegalEntityCustomer;
import bg.nbu.banksystem.customer.domain.Representative;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Maps Customer entities to their API-facing views.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CustomerMapper {

    private static String fullName(IndividualCustomer customer) {
        if (customer.getMiddleName() == null) {
            return customer.getFirstName() + " " + customer.getLastName();
        }

        return customer.getFirstName() + " " + customer.getMiddleName() + " " + customer.getLastName();
    }

    /**
     * Converts an individual Customer to its detailed view.
     *
     * @param customer the individual Customer
     * @return the detailed view
     */
    IndividualCustomerViewDto toIndividualView(IndividualCustomer customer);

    /**
     * Converts a legal-entity Customer to its detailed view, including all Representatives.
     *
     * @param customer the legal-entity Customer
     * @return the detailed view
     */
    LegalEntityCustomerViewDto toLegalEntityView(LegalEntityCustomer customer);

    /**
     * Converts an Address to its view.
     *
     * @param address the address
     * @return the address view
     */
    AddressViewDto toAddressView(Address address);

    /**
     * Converts a Representative to its view, exposing a linked individual Customer by identifier only.
     *
     * @param representative the Representative
     * @return the Representative view
     */
    @Mapping(target = "individualCustomerId", source = "individualCustomer.id")
    RepresentativeViewDto toRepresentativeView(Representative representative);

    /**
     * Converts a Customer of either type to its list and search summary.
     *
     * @param customer the Customer
     * @return the summary
     */
    default CustomerSummaryDto toSummary(Customer customer) {
        if (customer instanceof IndividualCustomer individual) {
            return new CustomerSummaryDto(individual.getId(), individual.getCustomerNumber(),
                    individual.getCustomerType(), fullName(individual), individual.getEgn(), individual.getStatus());
        }

        if (customer instanceof LegalEntityCustomer legalEntity) {
            return new CustomerSummaryDto(legalEntity.getId(), legalEntity.getCustomerNumber(),
                    legalEntity.getCustomerType(), legalEntity.getCompanyName(), legalEntity.getEik(),
                    legalEntity.getStatus());
        }

        throw new IllegalArgumentException("Unsupported Customer type: " + customer.getClass().getName());
    }

    /**
     * Converts an address request to the domain value object, which validates and normalizes it.
     *
     * @param address the address request
     * @return the Address
     */
    default Address toAddress(AddressDto address) {
        return Address.of(address.countryCode(), address.city(), address.postalCode(), address.addressLine1(),
                address.addressLine2());
    }
}