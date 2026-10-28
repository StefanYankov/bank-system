package bg.nbu.banksystem.customer.application;

import bg.nbu.banksystem.customer.application.dto.AddressDto;
import bg.nbu.banksystem.customer.application.dto.CustomerContactUpdateDto;
import bg.nbu.banksystem.customer.application.dto.EgnCorrectionDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerNameUpdateDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerRegistrationDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerViewDto;
import bg.nbu.banksystem.customer.domain.CustomerErrorCode;
import bg.nbu.banksystem.customer.domain.Egns;
import bg.nbu.banksystem.customer.domain.IndividualCustomer;
import bg.nbu.banksystem.customer.persistence.CustomerNumberGenerator;
import bg.nbu.banksystem.customer.persistence.IndividualCustomerRepository;
import bg.nbu.banksystem.starter.error.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Default implementation of the individual Customer use cases.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndividualCustomerServiceImpl implements IndividualCustomerService {

    private final IndividualCustomerRepository individualCustomerRepository;
    private final CustomerNumberGenerator customerNumberGenerator;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional
    public IndividualCustomerViewDto register(IndividualCustomerRegistrationDto registrationDto) {
        String egn = Egns.normalize(registrationDto.egn());

        if (individualCustomerRepository.existsByEgn(egn)) {
            throw new DomainException(CustomerErrorCode.CUSTOMER_EGN_ALREADY_REGISTERED);
        }

        IndividualCustomer customer = IndividualCustomer.register(customerNumberGenerator.generate(),
                registrationDto.email(), registrationDto.phone(), registrationDto.firstName(),
                registrationDto.middleName(), registrationDto.lastName(), egn,
                customerMapper.toAddress(registrationDto.residentialAddress()));

        IndividualCustomer saved = individualCustomerRepository.save(customer);
        log.info("Registered individual customer {}", saved.getCustomerNumber());

        return customerMapper.toIndividualView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public IndividualCustomerViewDto getById(UUID id) {
        return customerMapper.toIndividualView(findOrThrow(id));
    }

    @Override
    @Transactional
    public IndividualCustomerViewDto updateName(UUID id, IndividualCustomerNameUpdateDto nameUpdateDto) {
        IndividualCustomer customer = findOrThrow(id);
        customer.updateName(nameUpdateDto.firstName(), nameUpdateDto.middleName(), nameUpdateDto.lastName());

        return customerMapper.toIndividualView(customer);
    }

    @Override
    @Transactional
    public IndividualCustomerViewDto changeResidentialAddress(UUID id, AddressDto addressDto) {
        IndividualCustomer customer = findOrThrow(id);
        customer.changeResidentialAddress(customerMapper.toAddress(addressDto));

        return customerMapper.toIndividualView(customer);
    }

    @Override
    @Transactional
    public IndividualCustomerViewDto correctEgn(UUID id, EgnCorrectionDto egnCorrectionDto) {
        IndividualCustomer customer = findOrThrow(id);
        String egn = Egns.normalize(egnCorrectionDto.egn());

        if (individualCustomerRepository.existsByEgnAndIdNot(egn, id)) {
            throw new DomainException(CustomerErrorCode.CUSTOMER_EGN_ALREADY_REGISTERED);
        }

        customer.correctEgn(egn);
        log.info("Corrected EGN of customer {}", customer.getCustomerNumber());

        return customerMapper.toIndividualView(customer);
    }

    @Override
    @Transactional
    public IndividualCustomerViewDto updateContactInformation(UUID id, CustomerContactUpdateDto contactUpdateDto) {
        IndividualCustomer customer = findOrThrow(id);
        customer.updateContactInformation(contactUpdateDto.email(), contactUpdateDto.phone());

        return customerMapper.toIndividualView(customer);
    }

    @Override
    @Transactional
    public IndividualCustomerViewDto close(UUID id) {
        IndividualCustomer customer = findOrThrow(id);
        customer.close();
        log.info("Closed customer {}", customer.getCustomerNumber());

        return customerMapper.toIndividualView(customer);
    }

    @Override
    @Transactional
    public IndividualCustomerViewDto reopen(UUID id) {
        IndividualCustomer customer = findOrThrow(id);
        customer.reopen();
        log.info("Reopened customer {}", customer.getCustomerNumber());

        return customerMapper.toIndividualView(customer);
    }

    private IndividualCustomer findOrThrow(UUID id) {
        return individualCustomerRepository.findById(id)
                .orElseThrow(() -> new DomainException(CustomerErrorCode.CUSTOMER_NOT_FOUND));
    }
}