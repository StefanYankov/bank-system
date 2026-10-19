package bg.nbu.banksystem.customer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LegalEntityCustomer representatives (unit)")
class LegalEntityRepresentativeTest {

    private static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);

    private static LegalEntityCustomer legalEntityFixture() {
        return LegalEntityCustomer.register("CUS-000002", "contact@starter.example", "+35929201234",
                "Starter Financial Services", "123456789", registeredAddressFixture(),
                correspondenceAddressFixture());
    }

    private static Address registeredAddressFixture() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static Address correspondenceAddressFixture() {
        return Address.of("BG", "Sofia", "1000", "10 Business Park Street", "Office 4");
    }

    @Test
    @DisplayName("Happy Path: Should synchronize both sides when adding a Representative")
    void shouldSynchronizeRepresentativeRelationship() {
        // Arrange
        LegalEntityCustomer customer = legalEntityFixture();

        // Act
        Representative representative = customer.addRepresentative(null, "Serafim", "Ivanov", "Gerasimoff",
                RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);

        // Assert
        assertThat(representative.getLegalEntityCustomer()).isSameAs(customer);

        assertThat(customer.getRepresentatives()).containsExactly(representative);
    }

    @Test
    @DisplayName("Happy Path: Should support multiple Representatives")
    void shouldSupportMultipleRepresentatives() {
        // Arrange
        LegalEntityCustomer customer = legalEntityFixture();

        // Act
        Representative legalRepresentative = customer.addRepresentative(null, "Serafim", "Ivanov", "Gerasimoff",
                RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);

        Representative authorizedRepresentative = customer.addRepresentative(null, "Jane", null, "Doe",
                RepresentativeRole.AUTHORIZED_REPRESENTATIVE, VALID_FROM.plusMonths(1));

        // Assert
        assertThat(customer.getRepresentatives()).containsExactly(legalRepresentative, authorizedRepresentative);
    }

    @Test
    @DisplayName("Happy Path: Should retain ended Representatives in the history")
    void shouldRetainEndedRepresentative() {
        // Arrange
        LegalEntityCustomer customer = legalEntityFixture();

        Representative representative = customer.addRepresentative(null, "Serafim", "Ivanov", "Gerasimoff",
                RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);

        // Act
        representative.endRepresentation(LocalDate.of(2026, 12, 31));

        // Assert
        assertThat(customer.getRepresentatives()).containsExactly(representative);
        assertThat(representative.getValidTo()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("Error Case: Should not expose a mutable Representative collection")
    void shouldNotExposeMutableRepresentativeCollection() {
        // Arrange
        LegalEntityCustomer customer = legalEntityFixture();

        customer.addRepresentative(null, "Serafim", "Ivanov", "Gerasimoff", RepresentativeRole.LEGAL_REPRESENTATIVE,
                VALID_FROM);

        List<Representative> representatives = customer.getRepresentatives();

        // Act & Assert
        assertThatThrownBy(representatives::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Edge Case: Should return a snapshot of the Representative collection")
    void shouldReturnRepresentativeCollectionSnapshot() {
        // Arrange
        LegalEntityCustomer customer = legalEntityFixture();

        Representative firstRepresentative = customer.addRepresentative(null, "Serafim", "Ivanov", "Gerasimoff",
                RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);

        List<Representative> firstSnapshot = customer.getRepresentatives();

        // Act
        Representative secondRepresentative = customer.addRepresentative(null, "Jane", null, "Doe",
                RepresentativeRole.AUTHORIZED_REPRESENTATIVE, VALID_FROM.plusMonths(1));

        // Assert
        assertThat(firstSnapshot).containsExactly(firstRepresentative);
        assertThat(customer.getRepresentatives()).containsExactly(firstRepresentative, secondRepresentative);
    }
}