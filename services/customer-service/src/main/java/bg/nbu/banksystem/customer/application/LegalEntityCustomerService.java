package bg.nbu.banksystem.customer.application;

import bg.nbu.banksystem.customer.application.dto.AddressDto;
import bg.nbu.banksystem.customer.application.dto.CompanyNameUpdateDto;
import bg.nbu.banksystem.customer.application.dto.CustomerContactUpdateDto;
import bg.nbu.banksystem.customer.application.dto.EikCorrectionDto;
import bg.nbu.banksystem.customer.application.dto.LegalEntityCustomerRegistrationDto;
import bg.nbu.banksystem.customer.application.dto.LegalEntityCustomerViewDto;
import bg.nbu.banksystem.customer.application.dto.RepresentationEndDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeAdditionDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeCorrectionDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeViewDto;

import java.util.UUID;

/**
 * Application service for legal-entity Customer use cases, including their Representatives.
 */
public interface LegalEntityCustomerService {

    /**
     * Registers a new active legal-entity Customer.
     *
     * @param registrationDto registration data
     * @return the registered Customer
     * @throws bg.nbu.banksystem.starter.error.DomainException if the EIK is already registered
     */
    LegalEntityCustomerViewDto register(LegalEntityCustomerRegistrationDto registrationDto);

    /**
     * Retrieves a legal-entity Customer with all Representatives.
     *
     * @param id Customer identifier
     * @return the Customer
     * @throws bg.nbu.banksystem.starter.error.DomainException if no legal-entity Customer exists for the identifier
     */
    LegalEntityCustomerViewDto getById(UUID id);

    /**
     * Retrieves one Representative of a legal-entity Customer.
     *
     * @param id               Customer identifier
     * @param representativeId Representative identifier
     * @return the Representative
     * @throws bg.nbu.banksystem.starter.error.DomainException if the Customer does not exist, or the Representative
     *                                                         does not belong to it
     */
    RepresentativeViewDto getRepresentative(UUID id, UUID representativeId);

    /**
     * Updates the registered company name.
     *
     * @param id                   Customer identifier
     * @param companyNameUpdateDto new company name
     * @return the updated Customer
     * @throws bg.nbu.banksystem.starter.error.DomainException if the Customer does not exist or is closed
     */
    LegalEntityCustomerViewDto updateCompanyName(UUID id, CompanyNameUpdateDto companyNameUpdateDto);

    /**
     * Replaces the registered address.
     *
     * @param id         Customer identifier
     * @param addressDto new registered address
     * @return the updated Customer
     * @throws bg.nbu.banksystem.starter.error.DomainException if the Customer does not exist or is closed
     */
    LegalEntityCustomerViewDto changeRegisteredAddress(UUID id, AddressDto addressDto);

    /**
     * Replaces the correspondence address.
     *
     * @param id         Customer identifier
     * @param addressDto new correspondence address
     * @return the updated Customer
     * @throws bg.nbu.banksystem.starter.error.DomainException if the Customer does not exist or is closed
     */
    LegalEntityCustomerViewDto changeCorrespondenceAddress(UUID id, AddressDto addressDto);

    /**
     * Corrects an EIK entered incorrectly at registration.
     *
     * @param id               Customer identifier
     * @param eikCorrectionDto correct EIK
     * @return the updated Customer
     * @throws bg.nbu.banksystem.starter.error.DomainException if the Customer does not exist, is closed, or the EIK
     *                                                         belongs to another Customer
     */
    LegalEntityCustomerViewDto correctEik(UUID id, EikCorrectionDto eikCorrectionDto);

    /**
     * Updates the Customer's email and phone.
     *
     * @param id               Customer identifier
     * @param contactUpdateDto new contact information
     * @return the updated Customer
     * @throws bg.nbu.banksystem.starter.error.DomainException if the Customer does not exist or is closed
     */
    LegalEntityCustomerViewDto updateContactInformation(UUID id, CustomerContactUpdateDto contactUpdateDto);

    /**
     * Closes the Customer relationship. Representations are left unchanged.
     *
     * @param id Customer identifier
     * @return the closed Customer
     * @throws bg.nbu.banksystem.starter.error.DomainException if the Customer does not exist or is already closed
     */
    LegalEntityCustomerViewDto close(UUID id);

    /**
     * Reopens a closed Customer relationship.
     *
     * @param id Customer identifier
     * @return the reopened Customer
     * @throws bg.nbu.banksystem.starter.error.DomainException if the Customer does not exist or is already active
     */
    LegalEntityCustomerViewDto reopen(UUID id);

    /**
     * Adds a Representative, either linked to an individual Customer whose names are copied, or with the supplied
     * names.
     *
     * @param id          Customer identifier
     * @param additionDto Representative data
     * @return the added Representative
     * @throws bg.nbu.banksystem.starter.error.DomainException if the names break the naming rule, the Customer or the
     *                                                         linked individual Customer does not exist, or the
     *                                                         Customer is closed
     */
    RepresentativeViewDto addRepresentative(UUID id, RepresentativeAdditionDto additionDto);

    /**
     * Ends a Representative's authority.
     *
     * @param id               Customer identifier
     * @param representativeId Representative identifier
     * @param endDto           final valid date
     * @return the ended Representative
     * @throws bg.nbu.banksystem.starter.error.DomainException if the Customer or Representative does not exist, the
     *                                                         Customer is closed, or the period is invalid
     */
    RepresentativeViewDto endRepresentation(UUID id, UUID representativeId, RepresentationEndDto endDto);

    /**
     * Corrects a data-entry mistake in a Representative.
     *
     * @param id               Customer identifier
     * @param representativeId Representative identifier
     * @param correctionDto    corrected values
     * @return the corrected Representative
     * @throws bg.nbu.banksystem.starter.error.DomainException if the names break the naming rule, the Customer,
     *                                                         Representative or linked individual Customer does not
     *                                                         exist, the Customer is closed, or the period is invalid
     */
    RepresentativeViewDto correctRepresentative(UUID id, UUID representativeId,
                                                RepresentativeCorrectionDto correctionDto);
}