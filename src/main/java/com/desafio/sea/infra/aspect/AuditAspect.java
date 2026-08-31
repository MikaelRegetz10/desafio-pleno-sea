package com.desafio.sea.infra.aspect;

import com.desafio.sea.domain.AuditLog;
import com.desafio.sea.domain.User;
import com.desafio.sea.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(audit)")
    public Object logAudit(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = false;
        String errorMessage = null;
        Object result = null;
        String entityId = null;

        try {
            result = joinPoint.proceed();
            success = true;
            entityId = extractEntityId(joinPoint, result);
            return result;
        } catch (Throwable t) {
            errorMessage = t.getMessage();
            entityId = extractEntityIdFromArgs(joinPoint);
            throw t;
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            saveAuditLog(audit, joinPoint, durationMs, success, errorMessage, entityId);
        }
    }

    private void saveAuditLog(Audit audit, ProceedingJoinPoint joinPoint, long durationMs, boolean success, String errorMessage, String entityId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = null;
            String role = null;

            if (auth != null && auth.getPrincipal() instanceof User authenticatedUser) {
                user = authenticatedUser;
                role = user.getRole().name();
            }

            String actionName = audit.action().isBlank()
                    ? joinPoint.getSignature().getName()
                    : audit.action();

            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .role(role)
                    .action(actionName)
                    .entityId(entityId)
                    .durationMs(durationMs)
                    .success(success)
                    .errorMessage(errorMessage)
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Audit log saved: action={}, user={}, duration={}ms, success={}",
                    actionName, (user != null ? user.getEmail() : "ANONYMOUS"), durationMs, success);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    private String extractEntityId(ProceedingJoinPoint joinPoint, Object result) {
        String fromArgs = extractEntityIdFromArgs(joinPoint);
        if (fromArgs != null) return fromArgs;

        if (result instanceof ResponseEntity<?> responseEntity) {
            result = responseEntity.getBody();
        }

        if (result != null) {
            try {
                Method getIdMethod = result.getClass().getMethod("id");
                Object idVal = getIdMethod.invoke(result);
                if (idVal != null) return idVal.toString();
            } catch (Exception ignored) {
                try {
                    Method getIdMethod = result.getClass().getMethod("getId");
                    Object idVal = getIdMethod.invoke(result);
                    if (idVal != null) return idVal.toString();
                } catch (Exception ignored2) {}
            }
        }
        return null;
    }

    private String extractEntityIdFromArgs(ProceedingJoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof UUID uuid) {
                return uuid.toString();
            }
        }
        return null;
    }
}