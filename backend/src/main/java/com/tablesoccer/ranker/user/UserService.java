package com.tablesoccer.ranker.user;

import com.tablesoccer.ranker.match.MatchPlayerRepository;
import com.tablesoccer.ranker.match.MatchRepository;
import com.tablesoccer.ranker.ranking.EloSnapshotRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final EloSnapshotRepository eloSnapshotRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       MatchPlayerRepository matchPlayerRepository,
                       MatchRepository matchRepository,
                       EloSnapshotRepository eloSnapshotRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchRepository = matchRepository;
        this.eloSnapshotRepository = eloSnapshotRepository;
    }

    public List<UserDto> findAllActive() {
        return userRepository.findByActiveTrue().stream()
            .map(UserDto::from)
            .toList();
    }

    public UserDto findById(UUID id) {
        return UserDto.from(getUser(id));
    }

    public User getUser(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    public UserDto findCurrentUser(OAuth2User oAuth2User) {
        String sub = oAuth2User.getAttribute("sub");
        User user = userRepository.findByGoogleSub(sub)
            .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
        return UserDto.from(user);
    }

    public UserDto findCurrentUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
        return UserDto.from(user);
    }

    public User getCurrentUserEntity(OAuth2User oAuth2User) {
        String sub = oAuth2User.getAttribute("sub");
        return userRepository.findByGoogleSub(sub)
            .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
    }

    public User getCurrentUserEntity(Principal principal) {
        if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth) {
            return getCurrentUserEntity(oauth.getPrincipal());
        }
        return userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("User with username '" + request.username() + "' already exists");
        }
        String email = request.username() + "@local";
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User with username '" + request.username() + "' already exists");
        }

        var user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setEmail(email);
        // First user gets ADMIN role (countByRole is safer against race conditions than count)
        user.setRole(userRepository.countByRole(Role.ADMIN) == 0 ? Role.ADMIN : Role.PLAYER);
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Principal principal, ChangePasswordRequest request) {
        User user = getCurrentUserEntity(principal);
        if (user.getPassword() == null || !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserDto updateRole(UUID userId, Role role) {
        User user = getUser(userId);
        user.setRole(role);
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public UserDto updateEmail(UUID userId, String newEmail) {
        User user = getUser(userId);
        userRepository.findByEmail(newEmail).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new IllegalArgumentException("Email '" + newEmail + "' is already in use");
            }
        });
        user.setEmail(newEmail);
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public UserDto clearGoogleSub(UUID userId) {
        User user = getUser(userId);
        user.setGoogleSub(null);
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = getUser(userId);
        if (hasMatches(userId)) {
            throw new IllegalStateException("Cannot delete user who has participated in or recorded matches");
        }
        eloSnapshotRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    public boolean hasMatches(UUID userId) {
        return matchPlayerRepository.existsByUserId(userId)
            || matchRepository.existsByRecordedById(userId);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }
}
