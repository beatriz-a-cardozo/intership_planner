package com.interchangeplanner.finance.account.application;

import com.interchangeplanner.currency.CurrencyService;
import com.interchangeplanner.exchange.api.ExchangeRateResponse;
import com.interchangeplanner.exchange.application.CurrencyConverter;
import com.interchangeplanner.exchange.application.ExchangeRateService;
import com.interchangeplanner.finance.account.api.AccountRequest;
import com.interchangeplanner.finance.account.api.AccountResponse;
import com.interchangeplanner.finance.account.api.AccountsSummaryResponse;
import com.interchangeplanner.finance.account.api.BalanceRequest;
import com.interchangeplanner.finance.account.api.BalanceSnapshotResponse;
import com.interchangeplanner.finance.account.api.SavingsRequest;
import com.interchangeplanner.finance.account.domain.Account;
import com.interchangeplanner.finance.account.domain.AccountBalanceSnapshot;
import com.interchangeplanner.finance.account.domain.AccountBalanceSnapshotRepository;
import com.interchangeplanner.finance.account.domain.AccountRepository;
import com.interchangeplanner.finance.account.domain.AccountType;
import com.interchangeplanner.objective.domain.Objective;
import com.interchangeplanner.objective.application.ObjectiveService;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.shared.money.MoneyResponse;
import com.interchangeplanner.user.application.UserClock;
import com.interchangeplanner.user.application.UserService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    /** Conta criada no onboarding e ao guardar dinheiro sem escolher conta. */
    public static final String SAVINGS_ACCOUNT_NAME = "Reserva do intercâmbio";

    private final AccountRepository accounts;
    private final AccountBalanceSnapshotRepository snapshots;
    private final ObjectiveService objectives;
    private final CurrencyService currencies;
    private final ExchangeRateService exchange;
    private final UserService users;
    private final UserClock userClock;

    public AccountService(AccountRepository accounts, AccountBalanceSnapshotRepository snapshots,
            ObjectiveService objectives, CurrencyService currencies, ExchangeRateService exchange, UserService users,
            UserClock userClock) {
        this.accounts = accounts;
        this.snapshots = snapshots;
        this.objectives = objectives;
        this.currencies = currencies;
        this.exchange = exchange;
        this.users = users;
        this.userClock = userClock;
    }

    /** Conta com o saldo mais recente já resolvido. */
    public record AccountWithBalance(Account account, Money balance, LocalDate asOf) {
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(UUID userId) {
        return withBalances(accounts.findByUserIdOrderByArchivedAscNameAsc(userId)).stream()
                .map(AccountService::toResponse)
                .toList();
    }

    @Transactional
    public AccountResponse create(UUID userId, AccountRequest request) {
        if (!currencies.isSupported(request.currency())) {
            throw new BusinessRuleException("Moeda não suportada.", "currency");
        }
        validateObjective(userId, request.objectiveId());
        Account account = accounts.save(new Account(userId, request.name(), request.currency(), request.type(),
                request.objectiveId()));
        return toResponse(new AccountWithBalance(account, Money.zero(account.getCurrency()), null));
    }

    @Transactional
    public AccountResponse update(UUID userId, UUID id, AccountRequest request) {
        Account account = require(userId, id);
        if (!account.getCurrency().equals(request.currency())) {
            throw new BusinessRuleException("A moeda de uma conta não pode ser alterada. Crie uma nova conta.",
                    "currency");
        }
        validateObjective(userId, request.objectiveId());
        account.update(request.name(), request.type(), request.objectiveId(), Boolean.TRUE.equals(request.archived()));
        return toResponse(withBalances(List.of(account)).getFirst());
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        accounts.delete(require(userId, id));
    }

    @Transactional
    public BalanceSnapshotResponse recordBalance(UUID userId, UUID accountId, BalanceRequest request) {
        Account account = require(userId, accountId);
        LocalDate today = userClock.today(userId);
        LocalDate asOf = request.asOf() != null ? request.asOf() : today;
        if (asOf.isAfter(today)) {
            throw new BusinessRuleException("A data do saldo não pode estar no futuro.", "asOf");
        }
        AccountBalanceSnapshot snapshot = snapshots.findByAccountIdAndAsOf(accountId, asOf)
                .map(existing -> {
                    existing.changeBalance(request.balance());
                    return existing;
                })
                .orElseGet(() -> snapshots.save(new AccountBalanceSnapshot(accountId, asOf, request.balance())));
        return toResponse(snapshot, account.getCurrency());
    }

    /**
     * "Guardar para o intercâmbio": soma o valor (ou subtrai, se negativo) ao saldo de hoje de uma conta vinculada ao
     * objetivo. Sem conta escolhida, usa a primeira ativa na moeda ou cria a {@value #SAVINGS_ACCOUNT_NAME}.
     */
    @Transactional
    public AccountResponse addSavings(UUID userId, UUID objectiveId, SavingsRequest request) {
        Objective objective = objectives.requireOwned(userId, objectiveId);
        if (request.amount().signum() == 0) {
            throw new BusinessRuleException("Informe um valor diferente de zero.", "amount");
        }
        Account account = request.accountId() != null
                ? linkedAccount(userId, objectiveId, request.accountId())
                : savingsAccountFor(userId, objective,
                        request.currency() != null ? request.currency() : objective.getCurrency());
        AccountWithBalance current = withBalances(List.of(account)).getFirst();
        Money updated = current.balance().plus(Money.of(request.amount(), account.getCurrency()));
        if (updated.amount().signum() < 0) {
            throw new BusinessRuleException("A retirada é maior que o saldo da conta.", "amount");
        }
        recordBalance(userId, account.getId(), new BalanceRequest(null, updated.amount()));
        return toResponse(withBalances(List.of(account)).getFirst());
    }

    private Account linkedAccount(UUID userId, UUID objectiveId, UUID accountId) {
        Account account = require(userId, accountId);
        if (account.isArchived() || !objectiveId.equals(account.getObjectiveId())) {
            throw new BusinessRuleException("Escolha uma conta ativa vinculada a este objetivo.", "accountId");
        }
        return account;
    }

    private Account savingsAccountFor(UUID userId, Objective objective, String currency) {
        if (!currencies.isSupported(currency)) {
            throw new BusinessRuleException("Moeda não suportada.", "currency");
        }
        return accounts.findByUserIdAndObjectiveIdAndArchivedFalse(userId, objective.getId()).stream()
                .filter(a -> a.getCurrency().equals(currency))
                .findFirst()
                .orElseGet(() -> accounts.save(new Account(userId, SAVINGS_ACCOUNT_NAME, currency, AccountType.SAVINGS,
                        objective.getId())));
    }

    @Transactional(readOnly = true)
    public List<BalanceSnapshotResponse> history(UUID userId, UUID accountId) {
        Account account = require(userId, accountId);
        return snapshots.findByAccountIdOrderByAsOfDesc(accountId).stream()
                .map(s -> toResponse(s, account.getCurrency()))
                .toList();
    }

    /** Resumo de todas as contas ativas, convertido para a moeda principal do usuário. */
    @Transactional(readOnly = true)
    public AccountsSummaryResponse summary(UUID userId) {
        CurrencyConverter converter = exchange.converterFor(userId);
        String target = users.settings(userId).primaryCurrency();
        List<AccountWithBalance> active = withBalances(accounts.findByUserIdOrderByArchivedAscNameAsc(userId)).stream()
                .filter(a -> !a.account().isArchived())
                .toList();
        return summarize(active, target, converter);
    }

    /** Soma (convertida) do saldo das contas ativas vinculadas ao objetivo. */
    @Transactional(readOnly = true)
    public Money balanceForObjective(UUID userId, UUID objectiveId, String currency, CurrencyConverter converter) {
        return withBalances(accounts.findByUserIdAndObjectiveIdAndArchivedFalse(userId, objectiveId)).stream()
                .map(a -> converter.convert(a.balance(), currency))
                .reduce(Money.zero(currency), Money::plus);
    }

    public static AccountsSummaryResponse summarize(List<AccountWithBalance> active, String target,
            CurrencyConverter converter) {
        Map<String, Money> byCurrency = new TreeMap<>();
        Money total = Money.zero(target);
        for (AccountWithBalance a : active) {
            byCurrency.merge(a.balance().currency(), a.balance(), Money::plus);
            total = total.plus(converter.convert(a.balance(), target));
        }
        return new AccountsSummaryResponse(byCurrency.values().stream().map(MoneyResponse::of).toList(),
                MoneyResponse.of(total), active.size(), ExchangeRateResponse.of(converter));
    }

    private List<AccountWithBalance> withBalances(List<Account> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<UUID, AccountBalanceSnapshot> latest = snapshots.findLatestFor(list.stream().map(Account::getId).toList())
                .stream()
                .collect(Collectors.toMap(AccountBalanceSnapshot::getAccountId, Function.identity()));
        return list.stream().map(a -> {
            AccountBalanceSnapshot s = latest.get(a.getId());
            return s == null
                    ? new AccountWithBalance(a, Money.zero(a.getCurrency()), null)
                    : new AccountWithBalance(a, Money.of(s.getBalance(), a.getCurrency()), s.getAsOf());
        }).toList();
    }

    private Account require(UUID userId, UUID id) {
        return accounts.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Conta"));
    }

    private void validateObjective(UUID userId, UUID objectiveId) {
        if (objectiveId != null) {
            objectives.requireOwned(userId, objectiveId);
        }
    }

    private static AccountResponse toResponse(AccountWithBalance a) {
        Account acc = a.account();
        return new AccountResponse(acc.getId(), acc.getName(), acc.getCurrency(), acc.getType(), acc.getObjectiveId(),
                acc.isArchived(), MoneyResponse.of(a.balance()), a.asOf());
    }

    private static BalanceSnapshotResponse toResponse(AccountBalanceSnapshot s, String currency) {
        return new BalanceSnapshotResponse(s.getId(), s.getAsOf(), MoneyResponse.of(Money.of(s.getBalance(), currency)));
    }
}
