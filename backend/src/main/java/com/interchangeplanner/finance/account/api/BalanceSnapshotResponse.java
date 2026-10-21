package com.interchangeplanner.finance.account.api;

import com.interchangeplanner.shared.money.MoneyResponse;
import java.time.LocalDate;
import java.util.UUID;

public record BalanceSnapshotResponse(UUID id, LocalDate asOf, MoneyResponse balance) {
}
