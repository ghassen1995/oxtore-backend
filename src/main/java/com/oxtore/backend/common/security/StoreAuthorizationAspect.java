package com.oxtore.backend.common.security;

import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@Aspect
@Component
public class StoreAuthorizationAspect {

    private final StoreStaffRepository storeStaffRepository;

    public StoreAuthorizationAspect(StoreStaffRepository storeStaffRepository) {
        this.storeStaffRepository = storeStaffRepository;
    }

    @Around("@annotation(requireStoreRole)")
    public Object checkStoreRole(ProceedingJoinPoint joinPoint, RequireStoreRole requireStoreRole) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();

        Long storeId = "body".equals(requireStoreRole.storeIdSource())
                ? resolveFromBody(args, method)
                : resolveFromPath(args, paramNames, requireStoreRole.pathParam(), method);

        Long userId = resolveCurrentUserId();

        StoreStaff staff = storeStaffRepository.findByStore_IdAndUserId(storeId, userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "User %d is not staff of store %d".formatted(userId, storeId)));

        List<StaffRole> allowedRoles = Arrays.asList(requireStoreRole.value());
        if (!allowedRoles.contains(staff.getRole())) {
            throw new AccessDeniedException(
                    "User %d does not have required role in store %d (has: %s, needs one of: %s)"
                            .formatted(userId, storeId, staff.getRole(), allowedRoles));
        }

        return joinPoint.proceed();
    }

    private Long resolveFromPath(Object[] args, String[] paramNames, String pathParam, Method method) {
        if (pathParam == null || pathParam.isBlank()) {
            throw new IllegalStateException(
                    "@RequireStoreRole on %s uses storeIdSource=path but pathParam is not set"
                            .formatted(method.getName()));
        }
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(pathParam)) {
                if (args[i] instanceof Long l) {
                    return l;
                }
                if (args[i] instanceof String s) {
                    return Long.valueOf(s);
                }
                throw new IllegalStateException(
                        "Path parameter %s on %s is not a Long".formatted(pathParam, method.getName()));
            }
        }
        throw new IllegalStateException(
                "No path parameter named %s found on %s".formatted(pathParam, method.getName()));
    }

    private Long resolveFromBody(Object[] args, Method method) {
        for (Object arg : args) {
            if (arg instanceof HasStoreId hasStoreId) {
                return hasStoreId.resolveStoreId();
            }
        }
        throw new IllegalStateException(
                "@RequireStoreRole on %s uses storeIdSource=body but no argument implements HasStoreId"
                        .formatted(method.getName()));
    }

    private Long resolveCurrentUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attrs.getRequest();
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new AccessDeniedException("No authenticated user found on request");
        }
        return Long.valueOf(userId.toString());
    }
}