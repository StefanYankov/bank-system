package bg.nbu.banksystem.customer.domain;

/**
 * Describes how a person is authorized to represent a legal entity.
 */
public enum RepresentativeRole {

    /**
     * Represents the legal entity by virtue of its registered management.
     */
    LEGAL_REPRESENTATIVE,

    /**
     * Represents the legal entity through a separate authorization.
     */
    AUTHORIZED_REPRESENTATIVE
}