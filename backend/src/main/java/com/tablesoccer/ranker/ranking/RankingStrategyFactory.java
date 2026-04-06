package com.tablesoccer.ranker.ranking;

import com.tablesoccer.ranker.admin.AppSettingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RankingStrategyFactory {

    private final Map<LongTermAlgorithm, LongTermRankingStrategy> longTermStrategies;
    private final Map<MonthlyAlgorithm, MonthlyRankingStrategy> monthlyStrategies;
    private final AppSettingRepository settingRepository;

    public RankingStrategyFactory(List<LongTermRankingStrategy> longTermList,
                                  List<MonthlyRankingStrategy> monthlyList,
                                  AppSettingRepository settingRepository) {
        this.longTermStrategies = longTermList.stream()
            .collect(Collectors.toMap(LongTermRankingStrategy::algorithm, Function.identity()));
        this.monthlyStrategies = monthlyList.stream()
            .collect(Collectors.toMap(MonthlyRankingStrategy::algorithm, Function.identity()));
        this.settingRepository = settingRepository;
    }

    public LongTermRankingStrategy getActiveLongTermStrategy() {
        String value = settingRepository.findById("long_term_algorithm")
            .map(s -> s.getValue())
            .orElse("ELO");
        LongTermAlgorithm algo = LongTermAlgorithm.valueOf(value);
        return longTermStrategies.get(algo);
    }

    public MonthlyRankingStrategy getActiveMonthlyStrategy() {
        String value = settingRepository.findById("monthly_algorithm")
            .map(s -> s.getValue())
            .orElse("MONTHLY_ELO_GAIN");
        MonthlyAlgorithm algo = MonthlyAlgorithm.valueOf(value);
        return monthlyStrategies.get(algo);
    }

    public LongTermRankingStrategy getLongTermStrategy(LongTermAlgorithm algorithm) {
        return longTermStrategies.get(algorithm);
    }

    public MonthlyRankingStrategy getMonthlyStrategy(MonthlyAlgorithm algorithm) {
        return monthlyStrategies.get(algorithm);
    }
}
