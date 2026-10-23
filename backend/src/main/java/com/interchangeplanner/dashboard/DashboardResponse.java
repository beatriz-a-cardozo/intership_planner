package com.interchangeplanner.dashboard;

import com.interchangeplanner.exchange.api.ExchangeRateResponse;
import com.interchangeplanner.expense.api.ExpenseSummaryResponse;
import com.interchangeplanner.finance.account.api.AccountsSummaryResponse;
import com.interchangeplanner.objective.api.ObjectiveProgressResponse;
import com.interchangeplanner.objective.api.ObjectiveResponse;
import com.interchangeplanner.timeline.api.TimelineDtos.TimelineItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Tudo que a tela inicial precisa numa chamada, calculado com a MESMA cotação.
 * Sem objetivo (usuário novo), objective/progress/expenses vêm nulos.
 *
 * @param exchangeRate cotação EUR→BRL vigente para o usuário (com a de mercado para comparação)
 * @param upcoming     próximos itens da timeline (atrasados primeiro), sem os concluídos
 */
public record DashboardResponse(
        @Schema(types = {"object", "null"}) ObjectiveResponse objective,
        @Schema(types = {"object", "null"}) ObjectiveProgressResponse progress,
        AccountsSummaryResponse accounts,
        @Schema(types = {"object", "null"}) ExpenseSummaryResponse expenses,
        ExchangeRateResponse exchangeRate,
        ApplicationsOverview applications,
        List<TimelineItem> upcoming) {
}
