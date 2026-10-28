package bg.nbu.banksystem.customer.domain;

import bg.nbu.banksystem.starter.error.DomainException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LegalEntityCustomer representatives (unit)")
class LegalEntityRepresentativeTest {

    private static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate VALID_TO = LocalDate.of(2026, 12, 31);

    private static void assertDomainError(ThrowingCallable action, CustomerErrorCode expectedCode) {
        assertThatThrownBy(action).isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedCode));
    }

    private static Representative addLegalRepresentative(LegalEntityCustomer customer) {
        return customer.addRepresentative(null, "Serafim", "Ivanov", "Gerasimoff",
                RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);
    }

    private static LegalEntityCustomer legalEntityCustomer() {
        return LegalEntityCustomer.register("CUS-000002", "contact@starter.example", "+35929201234",
                "Starter Financial Services", "123456786", registeredAddress(), correspondenceAddress());
    }

    private static IndividualCustomer individualCustomer() {
        return IndividualCustomer.register("CUS-000001", "serafim.gerasimoff@example.com", "+359888123456", "Serafim",
                "Ivanov", "Gerasimoff", "6101047500", registeredAddress());
    }

    private static Address registeredAddress() {
        return Address.of("BG", "Sofia", "1000", "1 Alexander Battenberg Square", null);
    }

    private static Address correspondenceAddress() {
        return Address.of("BG", "Sofia", "1000", "10 Business Park Street", "Office 4");
    }

    @Test
    @DisplayName("Happy Path: Should synchronize both sides when adding a Representative")
    void shouldSynchronizeRepresentativeRelationship() {
        // Arrange
        LegalEntityCustomer customer = legalEntityCustomer();

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
        LegalEntityCustomer customer = legalEntityCustomer();

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
        LegalEntityCustomer customer = legalEntityCustomer();
        Representative representative = addLegalRepresentative(customer);

        // Act
        Representative ended = customer.endRepresentation(representative.getId(), VALID_TO);

        // Assert
        assertThat(ended).isSameAs(representative);
        assertThat(customer.getRepresentatives()).containsExactly(representative);
        assertThat(representative.getValidTo()).isEqualTo(VALID_TO);
    }

    @Test
    @DisplayName("Error Case: Should not expose a mutable Representative collection")
    void shouldNotExposeMutableRepresentativeCollection() {
        // Arrange
        LegalEntityCustomer customer = legalEntityCustomer();
        addLegalRepresentative(customer);
        List<Representative> representatives = customer.getRepresentatives();

        // Act & Assert
        assertThatThrownBy(representatives::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Edge Case: Should return a snapshot of the Representative collection")
    void shouldReturnRepresentativeCollectionSnapshot() {
        // Arrange
        LegalEntityCustomer customer = legalEntityCustomer();
        Representative firstRepresentative = addLegalRepresentative(customer);
        List<Representative> firstSnapshot = customer.getRepresentatives();

        // Act
        Representative secondRepresentative = customer.addRepresentative(null, "Jane", null, "Doe",
                RepresentativeRole.AUTHORIZED_REPRESENTATIVE, VALID_FROM.plusMonths(1));

        // Assert
        assertThat(firstSnapshot).containsExactly(firstRepresentative);
        assertThat(customer.getRepresentatives()).containsExactly(firstRepresentative, secondRepresentative);
    }

    @Test
    @DisplayName("Error Case: Should reject ending an unknown Representative")
    void shouldRejectEndingUnknownRepresentative() {
        // Arrange
        LegalEntityCustomer customer = legalEntityCustomer();
        addLegalRepresentative(customer);

        // Act & Assert
        assertDomainError(() -> customer.endRepresentation(UUID.randomUUID(), VALID_TO),
                CustomerErrorCode.REPRESENTATIVE_NOT_FOUND);
    }

    @Test
    @DisplayName("Error Case: Should reject a Representative belonging to another company")
    void shouldRejectRepresentativeOfAnotherCompany() {
        // Arrange
        LegalEntityCustomer customer = legalEntityCustomer();
        Representative otherRepresentative = addLegalRepresentative(legalEntityCustomer());

        // Act & Assert
        assertDomainError(() -> customer.endRepresentation(otherRepresentative.getId(), VALID_TO),
                CustomerErrorCode.REPRESENTATIVE_NOT_FOUND);
    }

    @Test
    @DisplayName("Error Case: Should reject adding a Representative to a closed company")
    void shouldRejectAddingToClosedCompany() {
        // Arrange
        LegalEntityCustomer customer = legalEntityCustomer();
        customer.close();

        // Act & Assert
        assertDomainError(() -> addLegalRepresentative(customer), CustomerErrorCode.CUSTOMER_CLOSED);
        assertThat(customer.getRepresentatives()).isEmpty();
    }

    @Test
    @DisplayName("Error Case: Should reject ending a representation of a closed company")
    void shouldRejectEndingOnClosedCompany() {
        // Arrange
        LegalEntityCustomer customer = legalEntityCustomer();
        Representative representative = addLegalRepresentative(customer);
        customer.close();

        // Act & Assert
        assertDomainError(() -> customer.endRepresentation(representative.getId(), VALID_TO),
                CustomerErrorCode.CUSTOMER_CLOSED);
        assertDomainError(() -> customer.endRepresentation(UUID.randomUUID(), VALID_TO),
                CustomerErrorCode.CUSTOMER_CLOSED);
        assertThat(representative.getValidTo()).isNull();
    }

    @Test
    @DisplayName("Happy Path: Should not affect representation when the linked individual Customer is closed")
    void shouldIgnoreClosedLinkedIndividual() {
        // Arrange
        LegalEntityCustomer customer = legalEntityCustomer();
        IndividualCustomer individual = individualCustomer();
        Representative representative = customer.addRepresentative(individual, "Serafim", "Ivanov", "Gerasimoff",
                RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM);

        // Act
        individual.close();
        customer.endRepresentation(representative.getId(), VALID_TO);

        // Assert
        assertThat(representative.getValidTo()).isEqualTo(VALID_TO);
    }

    @Test
    @DisplayName("Happy Path: Should correct a Representative through the company")
    void shouldCorrectRepresentativeThroughCompany() {
        // Arrange
        LegalEntityCustomer customer = legalEntityCustomer();
        Representative representative = addLegalRepresentative(customer);
        customer.endRepresentation(representative.getId(), VALID_TO);

        // Act
        Representative corrected = customer.correctRepresentative(representative.getId(), null, "Jane", null, "Doe",
                RepresentativeRole.AUTHORIZED_REPRESENTATIVE, VALID_FROM.plusMonths(1), null);

        // Assert
        assertThat(corrected).isSameAs(representative);
        assertThat(corrected.getFirstName()).isEqualTo("Jane");
        assertThat(corrected.getRole()).isEqualTo(RepresentativeRole.AUTHORIZED_REPRESENTATIVE);
        assertThat(corrected.getValidFrom()).isEqualTo(VALID_FROM.plusMonths(1));
        assertThat(corrected.getValidTo()).isNull();
        assertThat(customer.getRepresentatives()).containsExactly(representative);
    }

    @Test
    @DisplayName("Error Case: Should reject correcting an unknown Representative")
    void shouldRejectCorrectingUnknownRepresentative() {
        // Arrange
        LegalEntityCustomer customer = legalEntityCustomer();
        addLegalRepresentative(customer);

        // Act & Assert
        assertDomainError(() -> customer.correctRepresentative(UUID.randomUUID(), null, "Jane", null, "Doe",
                RepresentativeRole.LEGAL_REPRESENTATIVE, VALID_FROM, null), CustomerErrorCode.REPRESENTATIVE_NOT_FOUND);
    }

    @Test
    @DisplayName("Error Case: Should reject a correction on a closed company and change nothing")
    void shouldRejectCorrectionOnClosedCompany() {
        // Arrange
        LegalEntityCustomer customer = legalEntityCustomer();
        Representative representative = addLegalRepresentative(customer);
        customer.close();

        // Act & Assert
        assertDomainError(() -> customer.correctRepresentative(representative.getId(), null, "Jane", null, "Doe",
                RepresentativeRole.AUTHORIZED_REPRESENTATIVE, VALID_FROM, null), CustomerErrorCode.CUSTOMER_CLOSED);
        assertThat(representative.getFirstName()).isEqualTo("Serafim");
        assertThat(representative.getRole()).isEqualTo(RepresentativeRole.LEGAL_REPRESENTATIVE);
    }
}