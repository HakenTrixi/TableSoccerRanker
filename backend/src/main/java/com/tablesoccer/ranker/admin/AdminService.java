package com.tablesoccer.ranker.admin;

import com.tablesoccer.ranker.ranking.LongTermAlgorithm;
import com.tablesoccer.ranker.ranking.MonthlyAlgorithm;
import com.tablesoccer.ranker.ranking.RankingService;
import com.tablesoccer.ranker.user.Role;
import com.tablesoccer.ranker.user.UserDto;
import com.tablesoccer.ranker.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final AppSettingRepository settingRepository;
    private final UserService userService;
    private final RankingService rankingService;

    public AdminService(AppSettingRepository settingRepository,
                        UserService userService,
                        RankingService rankingService) {
        this.settingRepository = settingRepository;
        this.userService = userService;
        this.rankingService = rankingService;
    }

    public Map<String, String> getSettings() {
        return settingRepository.findAll().stream()
            .collect(Collectors.toMap(AppSetting::getKey, AppSetting::getValue));
    }

    @Transactional
    public void updateLongTermAlgorithm(LongTermAlgorithm algorithm) {
        var setting = settingRepository.findById("long_term_algorithm")
            .orElseThrow();
        setting.setValue(algorithm.name());
        settingRepository.save(setting);
        rankingService.recalculateAllRankings();
    }

    @Transactional
    public void updateMonthlyAlgorithm(MonthlyAlgorithm algorithm) {
        var setting = settingRepository.findById("monthly_algorithm")
            .orElseThrow();
        setting.setValue(algorithm.name());
        settingRepository.save(setting);
    }

    @Transactional
    public UserDto updateUserRole(UUID userId, Role role) {
        return userService.updateRole(userId, role);
    }

    @Transactional
    public void recalculateRankings() {
        rankingService.recalculateAllRankings();
    }

    public List<AdminUserDto> getAdminUsers() {
        return userService.findAll().stream()
            .map(user -> AdminUserDto.from(user, userService.hasMatches(user.getId())))
            .toList();
    }

    @Transactional
    public AdminUserDto updateUserEmail(UUID userId, String email) {
        userService.updateEmail(userId, email);
        var user = userService.getUser(userId);
        return AdminUserDto.from(user, userService.hasMatches(userId));
    }

    @Transactional
    public AdminUserDto clearUserGoogleSub(UUID userId) {
        userService.clearGoogleSub(userId);
        var user = userService.getUser(userId);
        return AdminUserDto.from(user, userService.hasMatches(userId));
    }

    @Transactional
    public void deleteUser(UUID userId) {
        userService.deleteUser(userId);
    }
}
