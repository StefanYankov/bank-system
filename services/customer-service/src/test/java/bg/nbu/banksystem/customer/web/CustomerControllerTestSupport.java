package bg.nbu.banksystem.customer.web;

import bg.nbu.banksystem.customer.application.CustomerService;
import bg.nbu.banksystem.customer.application.IndividualCustomerService;
import bg.nbu.banksystem.customer.application.LegalEntityCustomerService;
import bg.nbu.banksystem.customer.application.dto.CustomerSummaryDto;
import bg.nbu.banksystem.customer.domain.CustomerStatus;
import bg.nbu.banksystem.customer.domain.CustomerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Shared fixtures and HTTP-test infrastructure for Customer controller tests.
 */
abstract class CustomerControllerTestSupport {

    protected static final String BASE_URL = "/api/customers";
    protected static final UUID CUSTOMER_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10");
    protected static final String CUSTOMER_NUMBER = "CUS-000001";
    protected static final String EGN = "6101047500";
    protected static final String SUBJECT = "subject-001";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JsonMapper jsonMapper;

    @MockitoBean
    protected CustomerService customerService;

    @MockitoBean
    protected IndividualCustomerService individualCustomerService;

    @MockitoBean
    protected LegalEntityCustomerService legalEntityCustomerService;

    protected static CustomerSummaryDto individualSummary() {
        return new CustomerSummaryDto(CUSTOMER_ID, CUSTOMER_NUMBER, CustomerType.INDIVIDUAL,
                "Serafim Ivanov Gerasimoff", EGN, CustomerStatus.ACTIVE);
    }

    protected static JwtRequestPostProcessor jwtWithRole(String role) {
        return jwt().jwt(token -> token.subject(SUBJECT)).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}