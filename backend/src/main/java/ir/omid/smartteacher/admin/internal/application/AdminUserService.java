package ir.omid.smartteacher.admin.internal.application;

import ir.omid.smartteacher.admin.internal.api.dto.AdminUserResponse;
import ir.omid.smartteacher.admin.internal.api.dto.ChangeRoleRequest;
import ir.omid.smartteacher.admin.internal.api.dto.CreateAdminUserRequest;
import ir.omid.smartteacher.admin.internal.api.dto.ResetPasswordRequest;
import ir.omid.smartteacher.admin.internal.api.dto.UpdateAdminUserRequest;
import ir.omid.smartteacher.audit.ActorType;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.security.audit.SecurityAuditLog;
import ir.omid.smartteacher.security.audit.SecurityEvent;
import ir.omid.smartteacher.security.jwt.RefreshTokenRepository;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.user.User;
import ir.omid.smartteacher.user.UserRepository;
import ir.omid.smartteacher.user.UserStatus;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * مدیریت کاربران پنل ادمین — فهرست، CRUD پایه، وضعیت، نقش و رمز.
 *
 * <p><b>🔴 پس از غیرفعال/تعلیق/بازنشانی رمز، همه Refresh Tokenها باطل
 * می‌شوند</b> تا نشست‌های باز بلافاصله بی‌اعتبار شوند.
 *
 * <p>نقش‌های پلتفرمی از جدول {@code user_roles} با JDBC خوانده/نوشته
 * می‌شوند — موجودیت JPA جدا برای RBAC هنوز نیست و JDBC بدون وابستگی
 * مدولار اضافه کافی است.
 */
@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "fullName", "username", "status", "lastLoginAt");

    /** نقش‌های پلتفرمی قابل تخصیص از پنل ادمین. */
    private static final Set<String> ASSIGNABLE_PLATFORM_ROLES =
            Set.of("SUPER_ADMIN", "ADMIN");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityAuditLog securityAuditLog;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public AdminUserService(UserRepository userRepository,
                            RefreshTokenRepository refreshTokenRepository,
                            SecurityAuditLog securityAuditLog,
                            PasswordEncoder passwordEncoder,
                            JdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.securityAuditLog = securityAuditLog;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> list(int page, int size, String sort) {
        Pageable pageable = PageableFactory.of(
                page, size, sort, ALLOWED_SORT_FIELDS, "createdAt,desc");
        Page<User> users = userRepository.findByDeletedAtIsNull(pageable);
        return PageResponse.from(users, user -> toResponse(user, List.of()));
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(UUID userId) {
        User user = requireUser(userId);
        return toResponse(user, loadPlatformRoles(userId));
    }

    /**
     * ایجاد کاربر ستاد با رمز و نقش پلتفرمی.
     *
     * @param request ورودی ساخت
     * @return کاربر تازه‌ساخته با نقش‌ها
     */
    @Transactional
    public AdminUserResponse create(CreateAdminUserRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(username)) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE,
                    "این نام کاربری قبلاً ثبت شده است.");
        }

        String email = blankToNull(request.email());
        if (email != null
                && userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email).isPresent()) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE,
                    "این ایمیل قبلاً ثبت شده است.");
        }

        String mobile = blankToNull(request.mobile());
        if (mobile != null
                && userRepository.existsByMobileAndDeletedAtIsNull(mobile)) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE,
                    "این موبایل قبلاً ثبت شده است.");
        }

        String roleCode = request.role().trim();
        UUID roleId = requireAssignableRoleId(roleCode);

        User user = new User(request.fullName().trim());
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmail(email);
        user.setMobile(mobile);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        // ⚠️ JDBC بلافاصله بعد از save به user_roles می‌نویسد؛ بدون flush
        // شناسه کاربر هنوز در DB نیست و FK_ur_user می‌شکند.
        userRepository.flush();

        assignPlatformRole(user.getId(), roleId, TenantContext.currentUserId().orElse(null));

        auditSensitive("USER_CREATED", user.getId(), Map.of(
                "username", username,
                "role", roleCode));

        log.info("کاربر ستاد {} با نقش {} توسط ادمین ساخته شد", user.getId(), roleCode);
        return toResponse(user, List.of(roleCode));
    }

    /**
     * ویرایش پروفایل — نام، ایمیل، موبایل. رمز و نقش مسیر جدا دارند.
     *
     * @param userId  شناسه هدف
     * @param request فیلدهای قابل ویرایش
     * @return وضعیت تازه
     */
    @Transactional
    public AdminUserResponse update(UUID userId, UpdateAdminUserRequest request) {
        User user = requireUser(userId);

        String email = blankToNull(request.email());
        if (email != null) {
            userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                    .filter(other -> !other.getId().equals(userId))
                    .ifPresent(other -> {
                        throw new AppException(ErrorCode.DUPLICATE_RESOURCE,
                                "این ایمیل قبلاً ثبت شده است.");
                    });
        }

        String mobile = blankToNull(request.mobile());
        if (mobile != null) {
            userRepository.findByMobileAndDeletedAtIsNull(mobile)
                    .filter(other -> !other.getId().equals(userId))
                    .ifPresent(other -> {
                        throw new AppException(ErrorCode.DUPLICATE_RESOURCE,
                                "این موبایل قبلاً ثبت شده است.");
                    });
        }

        Map<String, Object> before = profileSnapshot(user);
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setMobile(mobile);

        Map<String, Object> updateMeta = new LinkedHashMap<>();
        updateMeta.put("before", before);
        updateMeta.put("after", profileSnapshot(user));
        auditSensitive("USER_UPDATED", userId, updateMeta);

        log.info("پروفایل کاربر {} توسط ادمین به‌روز شد", userId);
        return toResponse(user, loadPlatformRoles(userId));
    }

    /**
     * بازنشانی رمز — نشست‌ها باطل می‌شوند تا رمز قدیمی در دستگاه‌ها نماند.
     *
     * @param userId  شناسه هدف
     * @param request رمز جدید
     * @return وضعیت تازه (بدون رمز)
     */
    @Transactional
    public AdminUserResponse resetPassword(UUID userId, ResetPasswordRequest request) {
        User user = requireUser(userId);
        rejectSelfChange(userId, "بازنشانی رمز");

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        revokeSessions(userId, "بازنشانی رمز توسط ادمین");

        auditSensitive("PASSWORD_RESET", userId, Map.of("action", "password_reset"));

        log.info("رمز کاربر {} توسط ادمین بازنشانی شد", userId);
        return toResponse(user, loadPlatformRoles(userId));
    }

    /**
     * تغییر نقش پلتفرمی — ادمین نمی‌تواند نقش خودش را عوض کند
     * (جلوگیری از قفل‌شدن ناخواستهٔ سوپرادمین).
     *
     * @param userId  شناسه هدف
     * @param request نقش جدید
     * @return وضعیت تازه با نقش‌ها
     */
    @Transactional
    public AdminUserResponse changeRole(UUID userId, ChangeRoleRequest request) {
        User user = requireUser(userId);
        rejectSelfChange(userId, "تغییر نقش");

        String newRole = request.role().trim();
        UUID roleId = requireAssignableRoleId(newRole);
        List<String> previous = loadPlatformRoles(userId);

        clearPlatformRoles(userId);
        assignPlatformRole(userId, roleId, TenantContext.currentUserId().orElse(null));

        auditSensitive("ROLE_CHANGED", userId, Map.of(
                "fromRoles", previous,
                "toRole", newRole));

        log.info("نقش کاربر {} از {} به {} تغییر کرد", userId, previous, newRole);
        return toResponse(user, List.of(newRole));
    }

    /**
     * غیرفعال‌سازی دائمی حساب — ورود ممنوع تا enable.
     *
     * @param userId شناسه هدف
     * @return وضعیت تازه‌ی کاربر
     */
    @Transactional
    public AdminUserResponse disable(UUID userId) {
        User user = requireUser(userId);
        rejectSelfChange(userId, "غیرفعال‌سازی");

        UserStatus previous = user.getStatus();
        if (previous == UserStatus.DISABLED) {
            return toResponse(user, loadPlatformRoles(userId));
        }

        user.setStatus(UserStatus.DISABLED);
        revokeSessions(userId, "غیرفعال‌سازی حساب توسط ادمین");
        auditStatusChange(userId, previous, UserStatus.DISABLED, null);

        log.info("کاربر {} توسط ادمین غیرفعال شد", userId);
        return toResponse(user, loadPlatformRoles(userId));
    }

    /**
     * بازفعال‌سازی حساب — فقط از {@code DISABLED} یا {@code SUSPENDED}.
     *
     * @param userId شناسه هدف
     * @return وضعیت تازه‌ی کاربر
     */
    @Transactional
    public AdminUserResponse enable(UUID userId) {
        User user = requireUser(userId);
        rejectSelfChange(userId, "فعال‌سازی");

        UserStatus previous = user.getStatus();
        if (previous == UserStatus.ACTIVE) {
            return toResponse(user, loadPlatformRoles(userId));
        }

        if (previous != UserStatus.DISABLED && previous != UserStatus.SUSPENDED) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT,
                    "فقط حساب غیرفعال یا معلق قابل فعال‌سازی است.");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        auditStatusChange(userId, previous, UserStatus.ACTIVE, null);

        log.info("کاربر {} توسط ادمین فعال شد", userId);
        return toResponse(user, loadPlatformRoles(userId));
    }

    /**
     * حذف نرم — از فهرست‌ها خارج می‌شود؛ حذف خود ادمین ممنوع است.
     *
     * @param userId شناسه هدف
     */
    @Transactional
    public void softDelete(UUID userId) {
        User user = requireUser(userId);
        rejectSelfChange(userId, "حذف");

        user.softDelete();
        revokeSessions(userId, "حذف نرم حساب توسط ادمین");
        auditSensitive("USER_SOFT_DELETED", userId, Map.of(
                "username", user.getUsername() == null ? "" : user.getUsername()));

        log.info("کاربر {} توسط ادمین به‌صورت نرم حذف شد", userId);
    }

    /**
     * تعلیق موقت — ورود ممنوع تا enable؛ دلیل در حسابرسی ثبت می‌شود.
     *
     * @param userId شناسه هدف
     * @param reason دلیل تعلیق (برای حسابرسی؛ نباید راز داشته باشد)
     * @return وضعیت تازه‌ی کاربر
     */
    @Transactional
    public AdminUserResponse suspend(UUID userId, String reason) {
        User user = requireUser(userId);
        rejectSelfChange(userId, "تعلیق");

        UserStatus previous = user.getStatus();
        if (previous == UserStatus.SUSPENDED) {
            auditStatusChange(userId, previous, UserStatus.SUSPENDED, reason);
            return toResponse(user, loadPlatformRoles(userId));
        }

        user.setStatus(UserStatus.SUSPENDED);
        revokeSessions(userId, "تعلیق حساب توسط ادمین");
        auditStatusChange(userId, previous, UserStatus.SUSPENDED, reason);

        log.info("کاربر {} توسط ادمین معلق شد", userId);
        return toResponse(user, loadPlatformRoles(userId));
    }

    private User requireUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("کاربر"));
    }

    /**
     * ادمین نمی‌تواند وضعیت/نقش/رمز خودش را عوض کند — وگرنه با یک کلیک
     * از سامانه بیرون می‌ماند و راه بازگشت ندارد مگر دسترسی مستقیم به DB.
     */
    private static void rejectSelfChange(UUID targetUserId, String actionLabel) {
        UUID actorId = TenantContext.currentUserId().orElse(null);
        if (actorId != null && actorId.equals(targetUserId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "نمی‌توانید حساب خودتان را " + actionLabel + " کنید.");
        }
    }

    private void revokeSessions(UUID userId, String reason) {
        int revoked = refreshTokenRepository.revokeAllForUser(
                userId, Instant.now(), reason);
        if (revoked > 0) {
            securityAuditLog.record(
                    SecurityEvent.TOKEN_ALL_REVOKED,
                    null,
                    TenantContext.currentUserId().orElse(null),
                    ActorType.USER,
                    "User",
                    userId,
                    Map.of("revokedCount", String.valueOf(revoked), "reason", reason));
        }
    }

    private void auditStatusChange(UUID userId,
                                   UserStatus from,
                                   UserStatus to,
                                   String reason) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("fromStatus", from.name());
        metadata.put("toStatus", to.name());
        if (reason != null && !reason.isBlank()) {
            metadata.put("reason", reason);
        }
        auditSensitive("STATUS_CHANGED", userId, metadata);
    }

    private void auditSensitive(String actionDetail, UUID entityId, Map<String, Object> metadata) {
        Map<String, Object> meta = new LinkedHashMap<>(metadata);
        meta.put("detail", actionDetail);
        securityAuditLog.record(
                SecurityEvent.SENSITIVE_ACTION,
                null,
                TenantContext.currentUserId().orElse(null),
                ActorType.USER,
                "User",
                entityId,
                meta);
    }

    private List<String> loadPlatformRoles(UUID userId) {
        List<String> codes = jdbc.queryForList("""
                select r.code
                  from user_roles ur
                  join roles r on r.id = ur.role_id
                 where ur.user_id = ?
                   and ur.tenant_id is null
                 order by r.code
                """, String.class, userId);
        return List.copyOf(new LinkedHashSet<>(codes));
    }

    private UUID requireAssignableRoleId(String roleCode) {
        if (!ASSIGNABLE_PLATFORM_ROLES.contains(roleCode)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "نقش «" + roleCode + "» قابل تخصیص نیست.");
        }
        List<UUID> ids = jdbc.query("""
                select id from roles
                 where code = ? and level = 'PLATFORM' and is_system = true
                """, (rs, rowNum) -> (UUID) rs.getObject("id"), roleCode);
        if (ids.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "نقش «" + roleCode + "» در سامانه تعریف نشده است.");
        }
        return ids.getFirst();
    }

    private void clearPlatformRoles(UUID userId) {
        jdbc.update("""
                delete from user_roles
                 where user_id = ? and tenant_id is null
                """, userId);
    }

    private void assignPlatformRole(UUID userId, UUID roleId, UUID grantedBy) {
        jdbc.update("""
                insert into user_roles (id, user_id, role_id, tenant_id, granted_by)
                values (?, ?, ?, null, ?)
                """,
                UuidV7Generator.generate(), userId, roleId, grantedBy);
    }

    private static Map<String, Object> profileSnapshot(User user) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("fullName", user.getFullName());
        snap.put("email", user.getEmail());
        snap.put("mobile", user.getMobile());
        return snap;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    static AdminUserResponse toResponse(User user, List<String> roles) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getMobile(),
                user.getStatus(),
                roles == null ? List.of() : List.copyOf(roles),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
