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
import bg.nbu.banksystem.customer.domain.CustomerErrorCode;
import bg.nbu.banksystem.customer.domain.Eiks;
import bg.nbu.banksystem.customer.domain.IndividualCustomer;
import bg.nbu.banksystem.customer.domain.LegalEntityCustomer;
import bg.nbu.banksystem.customer.domain.Representative;
import bg.nbu.banksystem.customer.persistence.CustomerNumberGenerator;
import bg.nbu.banksystem.customer.persistence.IndividualCustomerRepository;
import bg.nbu.banksystem.customer.persistence.LegalEntityCustomerRepository;
import bg.nbu.banksystem.starter.error.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegalEntityCustomerServiceImpl implements LegalEntityCustomerService {

    static final String NAMES_NOT_ALLOWED = "Names must be empty when an individual Customer is linked";
    static final String NAMES_REQUIRED = "First and last name are required when no individual Customer is linked";

    private final LegalEntityCustomerRepository legalEntityCustomerRepository;
    private final IndividualCustomerRepository individualCustomerRepository;
    private final CustomerNumberGenerator customerNumberGenerator;
    private final CustomerMapper customerMapper;

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    @Override
    @Transactional
    public LegalEntityCustomerViewDto register(LegalEntityCustomerRegistrationDto registrationDto) {
        String eik = Eiks.normalize(registrationDto.eik());

        if (legalEntityCustomerRepository.existsByEik(eik)) {
            throw new DomainException(CustomerErrorCode.CUSTOMER_EIK_ALREADY_REGISTERED);
        }

        LegalEntityCustomer customer = LegalEntityCustomer.register(customerNumberGenerator.generate(),
                registrationDto.email(), registrationDto.phone(), registrationDto.companyName(), eik,
                customerMapper.toAddress(registrationDto.registeredAddress()),
                customerMapper.toAddress(registrationDto.correspondenceAddress()));

        LegalEntityCustomer saved = legalEntityCustomerRepository.save(customer);
        log.info("Registered legal-entity customer {}", saved.getCustomerNumber());

        return customerMapper.toLegalEntityView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LegalEntityCustomerViewDto getById(UUID id) {
        return customerMapper.toLegalEntityView(findOrThrow(id));
    }

    @Override
    @Transactional
    public LegalEntityCustomerViewDto updateCompanyName(UUID id, CompanyNameUpdateDto companyNameUpdateDto) {
        LegalEntityCustomer customer = findOrThrow(id);
        customer.updateCompanyName(companyNameUpdateDto.companyName());

        return customerMapper.toLegalEntityView(customer);
    }

    @Override
    @Transactional
    public LegalEntityCustomerViewDto changeRegisteredAddress(UUID id, AddressDto addressDto) {
        LegalEntityCustomer customer = findOrThrow(id);
        customer.changeRegisteredAddress(customerMapper.toAddress(addressDto));

        return customerMapper.toLegalEntityView(customer);
    }

    @Override
    @Transactional
    public LegalEntityCustomerViewDto changeCorrespondenceAddress(UUID id, AddressDto addressDto) {
        LegalEntityCustomer customer = findOrThrow(id);
        customer.changeCorrespondenceAddress(customerMapper.toAddress(addressDto));

        return customerMapper.toLegalEntityView(customer);
    }

    @Override
    @Transactional
    public LegalEntityCustomerViewDto correctEik(UUID id, EikCorrectionDto eikCorrectionDto) {
        LegalEntityCustomer customer = findOrThrow(id);
        String eik = Eiks.normalize(eikCorrectionDto.eik());

        if (legalEntityCustomerRepository.existsByEikAndIdNot(eik, id)) {
            throw new DomainException(CustomerErrorCode.CUSTOMER_EIK_ALREADY_REGISTERED);
        }

        customer.correctEik(eik);
        log.info("Corrected EIK of customer {}", customer.getCustomerNumber());

        return customerMapper.toLegalEntityView(customer);
    }

    @Override
    @Transactional
    public LegalEntityCustomerViewDto updateContactInformation(UUID id, CustomerContactUpdateDto contactUpdateDto) {
        LegalEntityCustomer customer = findOrThrow(id);
        customer.updateContactInformation(contactUpdateDto.email(), contactUpdateDto.phone());

        return customerMapper.toLegalEntityView(customer);
    }

    @Override
    @Transactional
    public LegalEntityCustomerViewDto close(UUID id) {
        LegalEntityCustomer customer = findOrThrow(id);
        customer.close();
        log.info("Closed customer {}", customer.getCustomerNumber());

        return customerMapper.toLegalEntityView(customer);
    }

    @Override
    @Transactional
    public LegalEntityCustomerViewDto reopen(UUID id) {
        LegalEntityCustomer customer = findOrThrow(id);
        customer.reopen();
        log.info("Reopened customer {}", customer.getCustomerNumber());

        return customerMapper.toLegalEntityView(customer);
    }

    @Override
    @Transactional
    public RepresentativeViewDto addRepresentative(UUID id, RepresentativeAdditionDto additionDto) {
        RepresentativeNames names = resolveNames(additionDto.individualCustomerId(), additionDto.firstName(),
                additionDto.middleName(), additionDto.lastName());
        LegalEntityCustomer customer = findOrThrow(id);

        Representative representative = customer.addRepresentative(names.individualCustomer(), names.firstName(),
                names.middleName(), names.lastName(), additionDto.role(), additionDto.validFrom());
        log.info("Added representative to customer {}", customer.getCustomerNumber());

        return customerMapper.toRepresentativeView(representative);
    }

    @Override
    @Transactional
    public RepresentativeViewDto endRepresentation(UUID id, UUID representativeId, RepresentationEndDto endDto) {
        LegalEntityCustomer customer = findOrThrow(id);
        Representative representative = customer.endRepresentation(representativeId, endDto.validTo());
        log.info("Ended representation for customer {}", customer.getCustomerNumber());

        return customerMapper.toRepresentativeView(representative);
    }

    @Override
    @Transactional
    public RepresentativeViewDto correctRepresentative(UUID id, UUID representativeId,
                                                       RepresentativeCorrectionDto correctionDto) {
        RepresentativeNames names = resolveNames(correctionDto.individualCustomerId(), correctionDto.firstName(),
                correctionDto.middleName(), correctionDto.lastName());
        LegalEntityCustomer customer = findOrThrow(id);

        Representative representative = customer.correctRepresentative(representativeId, names.individualCustomer(),
                names.firstName(), names.middleName(), names.lastName(), correctionDto.role(),
                correctionDto.validFrom(), correctionDto.validTo());
        log.info("Corrected representative for customer {}", customer.getCustomerNumber());

        return customerMapper.toRepresentativeView(representative);
    }

    private RepresentativeNames resolveNames(@Nullable UUID individualCustomerId, @Nullable String firstName,
                                             @Nullable String middleName, @Nullable String lastName) {
        if (individualCustomerId != null) {
            if (!isBlank(firstName) || !isBlank(middleName) || !isBlank(lastName)) {
                throw new DomainException(CustomerErrorCode.INVALID_REPRESENTATIVE_NAMES, NAMES_NOT_ALLOWED);
            }

            IndividualCustomer individual = individualCustomerRepository.findById(individualCustomerId)
                    .orElseThrow(() -> new DomainException(CustomerErrorCode.CUSTOMER_NOT_FOUND));

            return new RepresentativeNames(individual, individual.getFirstName(), individual.getMiddleName(),
                    individual.getLastName());
        }

        if (isBlank(firstName) || isBlank(lastName)) {
            throw new DomainException(CustomerErrorCode.INVALID_REPRESENTATIVE_NAMES, NAMES_REQUIRED);
        }

        return new RepresentativeNames(null, firstName, middleName, lastName);
    }

    private LegalEntityCustomer findOrThrow(UUID id) {
        return legalEntityCustomerRepository.findById(id)
                .orElseThrow(() -> new DomainException(CustomerErrorCode.CUSTOMER_NOT_FOUND));
    }

    private record RepresentativeNames(
            @Nullable
            IndividualCustomer individualCustomer,
            String firstName,
            @Nullable
            String middleName,
            String lastName
    ) {
    }
}