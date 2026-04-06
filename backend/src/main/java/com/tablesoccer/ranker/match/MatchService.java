package com.tablesoccer.ranker.match;

import com.tablesoccer.ranker.ranking.RankingService;
import com.tablesoccer.ranker.user.User;
import com.tablesoccer.ranker.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserService userService;
    private final RankingService rankingService;

    public MatchService(MatchRepository matchRepository, UserService userService,
                        RankingService rankingService) {
        this.matchRepository = matchRepository;
        this.userService = userService;
        this.rankingService = rankingService;
    }

    public Page<MatchDto> findAll(Pageable pageable) {
        return matchRepository.findAllByOrderByPlayedAtDesc(pageable).map(MatchDto::from);
    }

    public Page<MatchDto> findByPlayer(UUID userId, Pageable pageable) {
        return matchRepository.findByPlayerId(userId, pageable).map(MatchDto::from);
    }

    public MatchDto findById(UUID id) {
        Match match = matchRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Match not found: " + id));
        return MatchDto.from(match);
    }

    @Transactional
    public MatchDto recordMatch(MatchCreateRequest request, Principal principal) {
        validateDistinctPlayers(request);

        User recorder = userService.getCurrentUserEntity(principal);
        User yellowAtt = userService.getUser(request.yellowAttacker());
        User yellowDef = userService.getUser(request.yellowDefender());
        User whiteAtt = userService.getUser(request.whiteAttacker());
        User whiteDef = userService.getUser(request.whiteDefender());

        var match = new Match();
        match.setYellowScore(request.yellowScore());
        match.setWhiteScore(request.whiteScore());
        match.setPlayedAt(request.playedAt());
        match.setRecordedBy(recorder);

        match.addPlayer(yellowAtt, TeamColor.YELLOW, PlayerRole.ATTACKER);
        match.addPlayer(yellowDef, TeamColor.YELLOW, PlayerRole.DEFENDER);
        match.addPlayer(whiteAtt, TeamColor.WHITE, PlayerRole.ATTACKER);
        match.addPlayer(whiteDef, TeamColor.WHITE, PlayerRole.DEFENDER);

        Match saved = matchRepository.save(match);

        rankingService.updateRatingsAfterMatch(saved);

        return MatchDto.from(saved);
    }

    /**
     * Preview ELO changes for a hypothetical match without persisting anything.
     */
    public MatchDto previewMatch(MatchCreateRequest request) {
        validateDistinctPlayers(request);

        User yellowAtt = userService.getUser(request.yellowAttacker());
        User yellowDef = userService.getUser(request.yellowDefender());
        User whiteAtt = userService.getUser(request.whiteAttacker());
        User whiteDef = userService.getUser(request.whiteDefender());

        var match = new Match();
        match.setYellowScore(request.yellowScore());
        match.setWhiteScore(request.whiteScore());

        match.addPlayer(yellowAtt, TeamColor.YELLOW, PlayerRole.ATTACKER);
        match.addPlayer(yellowDef, TeamColor.YELLOW, PlayerRole.DEFENDER);
        match.addPlayer(whiteAtt, TeamColor.WHITE, PlayerRole.ATTACKER);
        match.addPlayer(whiteDef, TeamColor.WHITE, PlayerRole.DEFENDER);

        // Calculate ELO without saving — uses current ratings as-is
        rankingService.previewRatings(match);

        return MatchDto.from(match);
    }

    @Transactional
    public void deleteMatch(UUID id) {
        if (!matchRepository.existsById(id)) {
            throw new EntityNotFoundException("Match not found: " + id);
        }
        matchRepository.deleteById(id);
        rankingService.recalculateAllRankings();
    }

    private void validateDistinctPlayers(MatchCreateRequest request) {
        Set<UUID> playerIds = Set.of(
            request.yellowAttacker(), request.yellowDefender(),
            request.whiteAttacker(), request.whiteDefender()
        );
        if (playerIds.size() != 4) {
            throw new IllegalArgumentException("All four players must be different");
        }
    }
}
