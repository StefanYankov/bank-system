package bg.nbu.banksystem.customer.application;

import bg.nbu.banksystem.customer.application.dto.CustomerSummaryDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerViewDto;
import bg.nbu.banksystem.customer.application.dto.LegalEntityCustomerViewDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeViewDto;
import bg.nbu.banksystem.customer.domain.Address;
import bg.nbu.banksystem.customer.domain.Customer;
import bg.nbu.banksystem.customer.domain.CustomerStatus;
import bg.nbu.banksystem.customer.domain.CustomerType;
import bg.nbu.banksystem.customer.domain.IndividualCustomer;
import bg.nbu.banksystem.customer.domain.LegalEntityCustomer;
import bg.nbu.banksystem.customer.domain.Representative;
import bg.nbu.banksystem.customer.domain.RepresentativeRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("CustomerMapper (unit)")
class CustomerMapperTest {

    private static final String EGN = "6101047500";
    private static final String EIK = "123456786";
    private static final String COMPANY_NAME = "Starter Financial Services";
    private static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate VALID_TO = LocalDate.of(2026, 12, 31);

    private final CustomerMapper mapper = Mappers.getMapper(CustomerMapper.class);

    private static IndividualCustomer individualCustomer(String middleName) {
        return IndividualCustomer.register("CUS-000001", "serafim.gerasimoff@example.com", "+359888123456", "Serafim",
                middleName, "Gerasimoff", EGN, residentialAddress());
    }

    private static LegalEntityCustomer legalEntityCustomer() {
        return LegalEntityCustomer.register("CUS-000002", "contact@starter.example", "+35929201234", COMPANY_NAME, EIK,
                registeredAddress(), correspondenceAddress());
    }

    private static Address residentialAddress() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static Address registeredAddress() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static Address correspondenceAddress() {
        return Address.of("BG", "Sofia", "1000", "10 Business Park Street", "Office 4");
    }

    @Nested
    @DisplayName("Detail views")
    class DetailViews {

        @Test
        @DisplayName("Happy Path: Should map an individual Customer to its view")
        void shouldMapIndividualCustomerToView() {
            // Arrange
            IndividualCustomer customer = individualCustomer("Ivanov");

            // Act
            IndividualCustomerViewDto view = mapper.toIndividualView(customer);

            // Assert
            assertThat(view.id()).isEqualTo(customer.getId());
            assertThat(view.customerNumber()).isEqualTo("CUS-000001");
            assertThat(view.status()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(view.email()).isEqualTo("serafim.gerasimoff@example.com");
            assertThat(view.phone()).isEqualTo("+359888123456");
            assertThat(view.firstName()).isEqualTo("Serafim");
            assertThat(view.middleName()).isEqualTo("Ivanov");
            assertThat(view.lastName()).isEqualTo("Gerasimoff");
            assertThat(view.egn()).isEqualTo(EGN);
            assertThat(view.residentialAddress().city()).isEqualTo("Sofia");
            assertThat(view.residentialAddress().addressLine2()).isNull();
        }

        @Test
        @DisplayName("Happy Path: Should map a legal-entity Customer with addresses and Representatives")
        void shouldMapLegalEntityCustomerToView() {
            // Arrange
            LegalEntityCustomer customer = legalEntityCustomer();
            IndividualCustomer linkedIndividual = individualCustomer("Ivanov");
            Representative linked = customer.addRepresentative(linkedIndividual, "Serafim", "Ivanov", "Gerasimoff",
                    RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);
            customer.addRepresentative(null, "Jane", null, "Doe", RepresentativeRole.AUTHORIZED_REPRESENTATIVE,
                    VALID_FROM);
            customer.endRepresentation(linked.getId(), VALID_TO);

            // Act
            LegalEntityCustomerViewDto view = mapper.toLegalEntityView(customer);

            // Assert
            assertThat(view.id()).isEqualTo(customer.getId());
            assertThat(view.companyName()).isEqualTo(COMPANY_NAME);
            assertThat(view.eik()).isEqualTo(EIK);
            assertThat(view.registeredAddress().addressLine1()).isEqualTo("1 Alexander Battenberg Square");
            assertThat(view.correspondenceAddress().addressLine2()).isEqualTo("Office 4");
            assertThat(view.representatives()).hasSize(2);

            RepresentativeViewDto linkedView = view.representatives().getFirst();
            assertThat(linkedView.individualCustomerId()).isEqualTo(linkedIndividual.getId());
            assertThat(linkedView.role()).isEqualTo(RepresentativeRole.LEGAL_REPRESENTATIVE);
            assertThat(linkedView.validTo()).isEqualTo(VALID_TO);

            RepresentativeViewDto unlinkedView = view.representatives().get(1);
            assertThat(unlinkedView.individualCustomerId()).isNull();
            assertThat(unlinkedView.middleName()).isNull();
            assertThat(unlinkedView.validTo()).isNull();
        }
    }

    @Nested
    @DisplayName("Summaries")
    class Summaries {

        @Test
        @DisplayName("Happy Path: Should summarize an individual Customer with the full name and EGN")
        void shouldSummarizeIndividualWithMiddleName() {
            // Arrange
            IndividualCustomer customer = individualCustomer("Ivanov");

            // Act
            CustomerSummaryDto summary = mapper.toSummary(customer);

            // Assert
            assertThat(summary.id()).isEqualTo(customer.getId());
            assertThat(summary.customerNumber()).isEqualTo("CUS-000001");
            assertThat(summary.customerType()).isEqualTo(CustomerType.INDIVIDUAL);
            assertThat(summary.displayName()).isEqualTo("Serafim Ivanov Gerasimoff");
            assertThat(summary.identifier()).isEqualTo(EGN);
            assertThat(summary.status()).isEqualTo(CustomerStatus.ACTIVE);
        }

        @Test
        @DisplayName("Edge Case: Should omit a missing middle name from the display name")
        void shouldSummarizeIndividualWithoutMiddleName() {
            // Act
            CustomerSummaryDto summary = mapper.toSummary(individualCustomer(null));

            // Assert
            assertThat(summary.displayName()).isEqualTo("Serafim Gerasimoff");
        }

        @Test
        @DisplayName("Happy Path: Should summarize a legal-entity Customer with the company name and EIK")
        void shouldSummarizeLegalEntity() {
            // Arrange
            LegalEntityCustomer customer = legalEntityCustomer();
            customer.close();

            // Act
            CustomerSummaryDto summary = mapper.toSummary(customer);

            // Assert
            assertThat(summary.customerType()).isEqualTo(CustomerType.LEGAL_ENTITY);
            assertThat(summary.displayName()).isEqualTo(COMPANY_NAME);
            assertThat(summary.identifier()).isEqualTo(EIK);
            assertThat(summary.status()).isEqualTo(CustomerStatus.CLOSED);
        }

        @Test
        @DisplayName("Error Case: Should reject an unsupported Customer type")
        void shouldRejectUnsupportedCustomerType() {
            // Arrange
            Customer unknown = new Customer(UUID.randomUUID(), "CUS-000099", "unknown@example.com", "+359888000000") {
                @Override
                public CustomerType getCustomerType() {
                    return CustomerType.INDIVIDUAL;
                }
            };

            // Act & Assert
            assertThatIllegalArgumentException().isThrownBy(() -> mapper.toSummary(unknown))
                    .withMessageStartingWith("Unsupported Customer type");
        }
    }
}