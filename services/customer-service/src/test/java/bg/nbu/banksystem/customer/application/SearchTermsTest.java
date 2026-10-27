package bg.nbu.banksystem.customer.application;

import bg.nbu.banksystem.customer.domain.CustomerErrorCode;
import bg.nbu.banksystem.starter.error.DomainException;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SearchTerms (unit)")
class SearchTermsTest {

    @ParameterizedTest(name = "Edge Case: Should return no terms for \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("Edge Case: Should return no terms for a null or blank query")
    void shouldReturnNoTermsForBlankQuery(@Nullable String query) {
        // Act
        List<String> terms = SearchTerms.parse(query);

        // Assert
        assertThat(terms).isEmpty();
    }

    @Test
    @DisplayName("Happy Path: Should trim a single term")
    void shouldTrimSingleTerm() {
        // Act
        List<String> terms = SearchTerms.parse("  Gerasimoff  ");

        // Assert
        assertThat(terms).containsExactly("Gerasimoff");
    }

    @Test
    @DisplayName("Happy Path: Should split on any whitespace and keep the original order and case")
    void shouldSplitOnWhitespace() {
        // Act
        List<String> terms = SearchTerms.parse("Serafim   Gerasimoff\t6101");

        // Assert
        assertThat(terms).containsExactly("Serafim", "Gerasimoff", "6101");
    }

    @Test
    @DisplayName("Edge Case: Should accept exactly the maximum number of terms")
    void shouldAcceptMaximumTerms() {
        // Act
        List<String> terms = SearchTerms.parse("one two three four five");

        // Assert
        assertThat(terms).hasSize(SearchTerms.MAX_TERMS);
    }

    @Test
    @DisplayName("Error Case: Should reject more than the maximum number of terms")
    void shouldRejectTooManyTerms() {
        // Act & Assert
        assertThatThrownBy(() -> SearchTerms.parse("one two three four five six"))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(CustomerErrorCode.INVALID_SEARCH_QUERY);
                    assertThat(exception.getDetail()).contains(String.valueOf(SearchTerms.MAX_TERMS));
                });
    }

    @Test
    @DisplayName("Edge Case: Should return an immutable list")
    void shouldReturnImmutableList() {
        // Arrange
        List<String> terms = SearchTerms.parse("Gerasimoff");

        // Act & Assert
        assertThatThrownBy(terms::clear).isInstanceOf(UnsupportedOperationException.class);
    }
}