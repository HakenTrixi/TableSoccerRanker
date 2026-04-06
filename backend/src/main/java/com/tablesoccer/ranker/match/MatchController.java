package com.tablesoccer.ranker.match;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public Page<MatchDto> listMatches(@RequestParam(required = false) UUID playerId,
                                      @PageableDefault(size = 20) Pageable pageable) {
        if (playerId != null) {
            return matchService.findByPlayer(playerId, pageable);
        }
        return matchService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public MatchDto getMatch(@PathVariable UUID id) {
        return matchService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchDto createMatch(@Valid @RequestBody MatchCreateRequest request,
                                Principal principal) {
        return matchService.recordMatch(request, principal);
    }

    @PostMapping("/preview")
    public MatchDto previewMatch(@Valid @RequestBody MatchCreateRequest request) {
        return matchService.previewMatch(request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMatch(@PathVariable UUID id) {
        matchService.deleteMatch(id);
    }
}
