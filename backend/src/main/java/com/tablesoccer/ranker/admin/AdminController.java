package com.tablesoccer.ranker.admin;

import com.tablesoccer.ranker.dataimport.ExcelImportService;
import com.tablesoccer.ranker.ranking.LongTermAlgorithm;
import com.tablesoccer.ranker.ranking.MonthlyAlgorithm;
import com.tablesoccer.ranker.user.Role;
import com.tablesoccer.ranker.user.UserDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final ExcelImportService excelImportService;

    public AdminController(AdminService adminService, ExcelImportService excelImportService) {
        this.adminService = adminService;
        this.excelImportService = excelImportService;
    }

    @GetMapping("/settings")
    public Map<String, String> getSettings() {
        return adminService.getSettings();
    }

    @PutMapping("/settings/long-term-algorithm")
    public void updateLongTermAlgorithm(@RequestBody AlgorithmRequest request) {
        adminService.updateLongTermAlgorithm(LongTermAlgorithm.valueOf(request.algorithm()));
    }

    @PutMapping("/settings/monthly-algorithm")
    public void updateMonthlyAlgorithm(@RequestBody AlgorithmRequest request) {
        adminService.updateMonthlyAlgorithm(MonthlyAlgorithm.valueOf(request.algorithm()));
    }

    @GetMapping("/users")
    public List<AdminUserDto> listUsers() {
        return adminService.getAdminUsers();
    }

    @PutMapping("/users/{id}/role")
    public UserDto updateUserRole(@PathVariable UUID id, @RequestBody RoleRequest request) {
        return adminService.updateUserRole(id, Role.valueOf(request.role()));
    }

    @PutMapping("/users/{id}/email")
    public AdminUserDto updateUserEmail(@PathVariable UUID id, @RequestBody @Valid EmailRequest request) {
        return adminService.updateUserEmail(id, request.email());
    }

    @DeleteMapping("/users/{id}/google-sub")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearGoogleSub(@PathVariable UUID id) {
        adminService.clearUserGoogleSub(id);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        adminService.deleteUser(id);
    }

    @PostMapping("/rankings/recalculate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recalculateRankings() {
        adminService.recalculateRankings();
    }

    @PostMapping("/import")
    public ExcelImportService.ImportResult importFile(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.endsWith(".csv") || filename.endsWith(".txt"))) {
            return excelImportService.importFromCsv(file.getInputStream());
        }
        return excelImportService.importFromExcel(file.getInputStream());
    }

    record AlgorithmRequest(String algorithm) {}
    record RoleRequest(String role) {}
    record EmailRequest(@NotBlank @Email String email) {}
}
