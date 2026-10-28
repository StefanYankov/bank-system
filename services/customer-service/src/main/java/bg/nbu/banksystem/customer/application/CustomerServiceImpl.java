package bg.nbu.banksystem.customer.application;

import bg.nbu.banksystem.customer.application.dto.CustomerSummaryDto;
import bg.nbu.banksystem.customer.domain.CustomerErrorCode;
import bg.nbu.banksystem.customer.persistence.CustomerRepository;
import bg.nbu.banksystem.customer.persistence.CustomerSpecifications;
import bg.nbu.banksystem.starter.error.DomainException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "customerNumber");

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    private static Pageable withDefaultSort(Pageable pageable) {
        if (pageable.isUnpaged() || pageable.getSort().isSorted()) {
            return pageable;
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerSummaryDto> search(@Nullable String query, Pageable pageable) {
        List<String> terms = SearchTerms.parse(query);

        return customerRepository.findAll(CustomerSpecifications.matchesAllTerms(terms), withDefaultSort(pageable))
                .map(customerMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerSummaryDto getSummary(UUID id) {
        return customerRepository.findById(id)
                .map(customerMapper::toSummary)
                .orElseThrow(() -> new DomainException(CustomerErrorCode.CUSTOMER_NOT_FOUND));
    }
}