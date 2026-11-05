package bg.nbu.banksystem.account.persistence;

import bg.nbu.banksystem.account.domain.Account;
import bg.nbu.banksystem.account.domain.AccountStatus;
import bg.nbu.banksystem.account.domain.AccountTransaction;
import bg.nbu.banksystem.account.domain.AccountType;
import bg.nbu.banksystem.account.domain.Currency;
import bg.nbu.banksystem.account.domain.Iban;
import bg.nbu.banksystem.account.domain.TransactionType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration")
@Testcontainers
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AccountRepository (integration)")
class AccountRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
        DockerImageName.parse("postgres:18.6-alpine3.24"));

    private static final UUID OWNER_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10");
    private static final UUID OTHER_OWNER_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a11");
    private static final String AUTHENTICATED_SUBJECT = "authenticated-keycloak-subject";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcClient jdbcClient;

    private static Account openAccount(long sequence, UUID ownerCustomerId) {
        Iban iban = Iban.generate(AccountType.CURRENT, sequence);
        return Account.open(iban, ownerCustomerId, AccountType.CURRENT, Currency.EUR);
    }

    private static JwtAuthenticationToken authenticatedJwt(String subject) {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject(subject).build();
        return new JwtAuthenticationToken(jwt, List.of());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Happy Path: Should persist and reload an opened account")
        void shouldPersistAndReloadOpenedAccount() {
            // Arrange
            Account account = openAccount(1, OWNER_ID);
            accountRepository.saveAndFlush(account);
            entityManager.clear();

            // Act
            Account reloaded = accountRepository.findById(account.getId()).orElseThrow();

            // Assert
            assertThat(reloaded.getIban()).isEqualTo(account.getIban());
            assertThat(reloaded.getOwnerCustomerId()).isEqualTo(OWNER_ID);
            assertThat(reloaded.getType()).isEqualTo(AccountType.CURRENT);
            assertThat(reloaded.getCurrency()).isEqualTo(Currency.EUR);
            assertThat(reloaded.getBalance()).isEqualByComparingTo("0.00");
            assertThat(reloaded.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(reloaded.getTransactions()).isEmpty();
        }

        @Test
        @DisplayName("Happy Path: Should persist the balance and its transaction entries together")
        void shouldPersistBalanceAndTransactionsTogether() {
            // Arrange
            Account account = openAccount(2, OWNER_ID);
            accountRepository.saveAndFlush(account);
            account.credit(new BigDecimal("150.00"), Currency.EUR, "Initial deposit");
            account.debit(new BigDecimal("40.50"), Currency.EUR, "Cash withdrawal");

            // Act
            accountRepository.flush();
            entityManager.clear();

            // Assert
            Account reloaded = accountRepository.findById(account.getId()).orElseThrow();
            assertThat(reloaded.getBalance()).isEqualByComparingTo("109.50");
            assertThat(reloaded.getTransactions()).extracting(AccountTransaction::getType)
                .containsExactly(TransactionType.CREDIT, TransactionType.DEBIT);
            assertThat(reloaded.getTransactions()).extracting(AccountTransaction::getResultingBalance)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("150.00"), new BigDecimal("109.50"));
        }

        @Test
        @DisplayName("Happy Path: Should persist a closed account")
        void shouldPersistClosedAccount() {
            // Arrange
            Account account = openAccount(3, OWNER_ID);
            accountRepository.saveAndFlush(account);

            // Act
            account.close();
            accountRepository.flush();
            entityManager.clear();

            // Assert
            assertThat(accountRepository.findById(account.getId()).orElseThrow().getStatus()).isEqualTo(
                AccountStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("Queries")
    class Queries {

        @Test
        @DisplayName("Happy Path: Should find an account by IBAN")
        void shouldFindByIban() {
            // Arrange
            Account account = accountRepository.saveAndFlush(openAccount(4, OWNER_ID));
            entityManager.clear();

            // Act & Assert
            assertThat(accountRepository.findByIban(account.getIban())).get().extracting(Account::getId)
                .isEqualTo(account.getId());
        }

        @Test
        @DisplayName("Edge Case: Should return an empty result for an unknown IBAN")
        void shouldReturnEmptyForUnknownIban() {
            // Arrange
            String unknownIban = Iban.generate(AccountType.SAVINGS, 99_999_999L).value();

            // Act & Assert
            assertThat(accountRepository.findByIban(unknownIban)).isEmpty();
        }

        @Test
        @DisplayName("Happy Path: Should list only the accounts of one owner")
        void shouldListAccountsByOwner() {
            // Arrange
            accountRepository.saveAndFlush(openAccount(5, OWNER_ID));
            accountRepository.saveAndFlush(openAccount(6, OWNER_ID));
            accountRepository.saveAndFlush(openAccount(7, OTHER_OWNER_ID));
            entityManager.clear();

            // Act
            Page<Account> page = accountRepository.findByOwnerCustomerId(OWNER_ID, PageRequest.of(0, 10));

            // Assert
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).extracting(Account::getOwnerCustomerId).containsOnly(OWNER_ID);
        }
    }

    @Nested
    @DisplayName("Auditing and concurrency")
    class AuditingAndConcurrency {

        @Test
        @DisplayName("Happy Path: Should populate audit fields with the SYSTEM actor and initialize the version")
        void shouldPopulateSystemAuditFields() {
            // Arrange
            Account account = openAccount(8, OWNER_ID);

            // Act
            accountRepository.saveAndFlush(account);

            // Assert
            assertThat(account.getCreatedAt()).isNotNull();
            assertThat(account.getCreatedBy()).isEqualTo(JpaAuditingConfig.SYSTEM_ACTOR);
            assertThat(account.getUpdatedBy()).isEqualTo(JpaAuditingConfig.SYSTEM_ACTOR);
            assertThat(account.getVersion()).isZero();
        }

        @Test
        @DisplayName("Happy Path: Should record the authenticated actor and advance the version on a balance change")
        void shouldRecordAuthenticatedActorOnBalanceChange() {
            // Arrange
            Account account = openAccount(9, OWNER_ID);
            accountRepository.saveAndFlush(account);
            Long initialVersion = account.getVersion();
            SecurityContextHolder.getContext().setAuthentication(authenticatedJwt(AUTHENTICATED_SUBJECT));

            // Act
            account.credit(new BigDecimal("10.00"), Currency.EUR, "Deposit");
            accountRepository.flush();

            // Assert
            assertThat(account.getUpdatedBy()).isEqualTo(AUTHENTICATED_SUBJECT);
            assertThat(account.getCreatedBy()).isEqualTo(JpaAuditingConfig.SYSTEM_ACTOR);
            assertThat(account.getVersion()).isGreaterThan(initialVersion);
            assertThat(account.getTransactions().getFirst().getCreatedBy()).isEqualTo(AUTHENTICATED_SUBJECT);
        }

        @Test
        @DisplayName("Error Case: Should reject a balance change based on a stale account version")
        void shouldRejectStaleBalanceChange() {
            // Arrange
            Account account = openAccount(10, OWNER_ID);
            accountRepository.saveAndFlush(account);
            jdbcClient.sql("UPDATE accounts SET version = version + 1 WHERE id = :id").param("id", account.getId())
                .update();
            account.credit(new BigDecimal("25.00"), Currency.EUR, "Deposit");

            // Act & Assert
            assertThatThrownBy(() -> accountRepository.flush()).isInstanceOf(OptimisticLockingFailureException.class);
        }
    }

    @Nested
    @DisplayName("Database constraints")
    class DatabaseConstraints {

        @Test
        @DisplayName("Error Case: Should reject a duplicate IBAN")
        void shouldRejectDuplicateIban() {
            // Arrange
            accountRepository.saveAndFlush(openAccount(11, OWNER_ID));
            Account duplicate = openAccount(11, OTHER_OWNER_ID);

            // Act & Assert
            assertThatThrownBy(() -> accountRepository.saveAndFlush(duplicate)).isInstanceOf(
                DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Error Case: Should reject a negative balance at the database level")
        void shouldRejectNegativeBalance() {
            // Arrange
            Account account = accountRepository.saveAndFlush(openAccount(12, OWNER_ID));

            // Act & Assert
            assertThatThrownBy(
                () -> jdbcClient.sql("UPDATE accounts SET balance = -1.00 WHERE id = :id").param("id", account.getId())
                    .update()).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Error Case: Should reject an unknown status at the database level")
        void shouldRejectUnknownStatus() {
            // Arrange
            Account account = accountRepository.saveAndFlush(openAccount(13, OWNER_ID));

            // Act & Assert
            assertThatThrownBy(() -> jdbcClient.sql("UPDATE accounts SET status = 'FROZEN' WHERE id = :id")
                .param("id", account.getId()).update()).isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
