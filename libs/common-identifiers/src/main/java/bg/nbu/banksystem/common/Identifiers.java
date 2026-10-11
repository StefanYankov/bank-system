package bg.nbu.banksystem.common;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/**
 * Generates application-assigned identifiers shared by bank-system services.
 */
public final class Identifiers {

    private Identifiers() {
        throw new IllegalStateException(
                "Identifier utility class must not be instantiated"
        );
    }

    /**
     * Generates a time-ordered UUIDv7 identifier.
     *
     * <p>
     * UUIDv7 preserves decentralised identifier generation while improving
     * database index locality compared with randomly ordered UUIDv4 values.
     * </p>
     *
     * @return a newly generated UUIDv7 identifier
     */
    public static UUID newUuid() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}