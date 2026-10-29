package bg.nbu.banksystem.customer.web;

import bg.nbu.banksystem.customer.application.CustomerService;
import bg.nbu.banksystem.customer.application.IndividualCustomerService;
import bg.nbu.banksystem.customer.application.LegalEntityCustomerService;
import bg.nbu.banksystem.customer.application.dto.AddressDto;
import bg.nbu.banksystem.customer.application.dto.AddressViewDto;
import bg.nbu.banksystem.customer.application.dto.CustomerSummaryDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerRegistrationDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerViewDto;
import bg.nbu.banksystem.customer.application.dto.LegalEntityCustomerRegistrationDto;
import bg.nbu.banksystem.customer.application.dto.LegalEntityCustomerViewDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeAdditionDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeViewDto;
import bg.nbu.banksystem.customer.domain.CustomerStatus;
import bg.nbu.banksystem.customer.domain.CustomerType;
import bg.nbu.banksystem.customer.domain.RepresentativeRole;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

abstract class CustomerControllerTestSupport {

    protected static final String BASE_URL = "/api/customers";
    protected static final String INDIVIDUALS_URL = BASE_URL + "/individuals";
    protected static final String LEGAL_ENTITIES_URL = BASE_URL + "/legal-entities";
    protected static final UUID CUSTOMER_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10");
    protected static final UUID REPRESENTATIVE_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a11");
    protected static final String CUSTOMER_NUMBER = "CUS-000001";
    protected static final String EGN = "6101047500";
    protected static final String EIK = "123456786";
    protected static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
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

    protected static IndividualCustomerViewDto individualView(CustomerStatus status) {
        return new IndividualCustomerViewDto(CUSTOMER_ID, CUSTOMER_NUMBER, status, "serafim.gerasimoff@example.com",
                "+359888123456", "Serafim", "Ivanov", "Gerasimoff", EGN, addressView());
    }

    protected static IndividualCustomerRegistrationDto individualRegistration(String egn) {
        return new IndividualCustomerRegistrationDto("serafim.gerasimoff@example.com", "+359888123456", "Serafim",
                "Ivanov", "Gerasimoff", egn, address());
    }

    protected static LegalEntityCustomerViewDto legalEntityView(CustomerStatus status) {
        return new LegalEntityCustomerViewDto(CUSTOMER_ID, CUSTOMER_NUMBER, status, "contact@starter.example",
                "+35929201234", "Starter Financial Services", EIK, addressView(), addressView(),
                List.of(representativeView(null)));
    }

    protected static LegalEntityCustomerRegistrationDto legalEntityRegistration(String eik) {
        return new LegalEntityCustomerRegistrationDto("contact@starter.example", "+35929201234",
                "Starter Financial Services", eik, address(), address());
    }

    protected static RepresentativeViewDto representativeView(@Nullable LocalDate validTo) {
        return new RepresentativeViewDto(REPRESENTATIVE_ID, null, "Jane", null, "Doe",
                RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM, validTo);
    }

    protected static RepresentativeAdditionDto unlinkedAddition() {
        return new RepresentativeAdditionDto(null, "Jane", null, "Doe", RepresentativeRole.LEGAL_REPRESENTATIVE,
                VALID_FROM);
    }

    protected static AddressDto address() {
        return new AddressDto("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    protected static AddressViewDto addressView() {
        return new AddressViewDto("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }
}