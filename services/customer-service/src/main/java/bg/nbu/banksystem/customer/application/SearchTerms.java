package bg.nbu.banksystem.customer.application;

import bg.nbu.banksystem.customer.domain.CustomerErrorCode;
import bg.nbu.banksystem.starter.error.DomainException;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits a free-text Customer search query into search terms.
 */
final class SearchTerms {

    static final int MAX_TERMS = 5;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private SearchTerms() {
    }

    /**
     * Parses a search query into terms.
     *
     * @param query the raw search query, possibly {@code null} or blank
     * @return the search terms in their original order; empty when the query is blank
     * @throws DomainException with {@link CustomerErrorCode#INVALID_SEARCH_QUERY} if the query contains more than
     *                         {@link #MAX_TERMS} terms
     */
    static List<String> parse(@Nullable String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String[] terms = WHITESPACE.split(query.trim());

        if (terms.length > MAX_TERMS) {
            throw new DomainException(CustomerErrorCode.INVALID_SEARCH_QUERY,
                    "Search query must not contain more than " + MAX_TERMS + " terms");
        }
        return List.of(terms);
    }
}