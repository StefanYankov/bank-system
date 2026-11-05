package bg.nbu.banksystem.customer.persistence;

import bg.nbu.banksystem.customer.domain.Customer;
import bg.nbu.banksystem.customer.domain.IndividualCustomer;
import bg.nbu.banksystem.customer.domain.LegalEntityCustomer;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Query conditions for searching Customers of both types.
 */
public final class CustomerSpecifications {

    private static final char ESCAPE_CHARACTER = '\\';

    private CustomerSpecifications() {
    }

    /**
     * Matches Customers for which every term matches at least one searchable field.
     *
     * @param terms non-blank search terms
     * @return the search condition
     */
    public static Specification<Customer> matchesAllTerms(List<String> terms) {
        return (root, query, builder) -> {
            Predicate[] termPredicates = terms.stream().map(term -> matchesTerm(term, root, query, builder))
                .toArray(Predicate[]::new);

            return builder.and(termPredicates);
        };
    }

    private static Predicate matchesTerm(String term, Root<Customer> root, CriteriaQuery<?> query,
                                         CriteriaBuilder builder) {
        String containsPattern = "%" + escape(term.toLowerCase(Locale.ROOT)) + "%";
        String startsWithPattern = escape(term) + "%";

        Subquery<UUID> individuals = query.subquery(UUID.class);
        Root<IndividualCustomer> individual = individuals.from(IndividualCustomer.class);
        individuals.select(individual.get("id")).where(
            builder.or(containsIgnoringCase(builder, individual.get("firstName"), containsPattern),
                containsIgnoringCase(builder, individual.get("middleName"), containsPattern),
                containsIgnoringCase(builder, individual.get("lastName"), containsPattern),
                builder.like(individual.get("egn"), startsWithPattern, ESCAPE_CHARACTER)));

        Subquery<UUID> legalEntities = query.subquery(UUID.class);
        Root<LegalEntityCustomer> legalEntity = legalEntities.from(LegalEntityCustomer.class);
        legalEntities.select(legalEntity.get("id")).where(
            builder.or(containsIgnoringCase(builder, legalEntity.get("companyName"), containsPattern),
                builder.like(legalEntity.get("eik"), startsWithPattern, ESCAPE_CHARACTER)));

        return builder.or(containsIgnoringCase(builder, root.get("customerNumber"), containsPattern),
            root.get("id").in(individuals), root.get("id").in(legalEntities));
    }

    private static Predicate containsIgnoringCase(CriteriaBuilder builder, Expression<String> field,
                                                  String lowerCasePattern) {
        return builder.like(builder.lower(field), lowerCasePattern, ESCAPE_CHARACTER);
    }

    private static String escape(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
