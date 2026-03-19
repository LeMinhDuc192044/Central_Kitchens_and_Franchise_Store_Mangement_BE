package com.example.Central_Kitchens_and_Franchise_Store_BE.util;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class OrderStatusValidator {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = new HashMap<>();

    // ── Các status chỉ GHN webhook mới được set ──────────────────
    private static final Set<OrderStatus> GHN_ONLY_STATUSES = Set.of(
            OrderStatus.READY_TO_PICK,
            OrderStatus.PICKING,
            OrderStatus.PICKED,
            OrderStatus.DELIVERING,
            OrderStatus.DELIVERED,
            OrderStatus.DELIVERY_FAILED,
            OrderStatus.WAITING_TO_RETURN,
            OrderStatus.RETURNED
    );

    static {
        // === KITCHEN FLOW ===
        VALID_TRANSITIONS.put(OrderStatus.PENDING,
                Set.of(OrderStatus.IN_PROGRESS,OrderStatus.WAITING_FOR_UPDATE,OrderStatus.WAITING_FOR_PRODUCTION, OrderStatus.CANCELLED));

        VALID_TRANSITIONS.put(OrderStatus.IN_PROGRESS,
                Set.of(OrderStatus.COOKING_DONE, OrderStatus.WAITING_FOR_UPDATE, OrderStatus.CANCELLED));

        VALID_TRANSITIONS.put(OrderStatus.WAITING_FOR_UPDATE,
                Set.of(OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED));

        VALID_TRANSITIONS.put(OrderStatus.WAITING_FOR_PRODUCTION,
                Set.of(OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED));

        VALID_TRANSITIONS.put(OrderStatus.COOKING_DONE,
                Set.of(OrderStatus.READY_TO_PICK)); // tự động, do system trigger

        // === GHN WEBHOOK FLOW ===
        VALID_TRANSITIONS.put(OrderStatus.READY_TO_PICK,
                Set.of(OrderStatus.PICKING));

        VALID_TRANSITIONS.put(OrderStatus.PICKING,
                Set.of(OrderStatus.PICKED));

        VALID_TRANSITIONS.put(OrderStatus.PICKED,
                Set.of(OrderStatus.DELIVERING));

        VALID_TRANSITIONS.put(OrderStatus.DELIVERING,
                Set.of(OrderStatus.DELIVERED, OrderStatus.DELIVERY_FAILED));

        VALID_TRANSITIONS.put(OrderStatus.DELIVERY_FAILED,
                Set.of(OrderStatus.WAITING_TO_RETURN));

        VALID_TRANSITIONS.put(OrderStatus.WAITING_TO_RETURN,
                Set.of(OrderStatus.RETURNED));

        // === TERMINAL STATES ===
        VALID_TRANSITIONS.put(OrderStatus.DELIVERED,  Set.of());
        VALID_TRANSITIONS.put(OrderStatus.RETURNED,   Set.of());
        VALID_TRANSITIONS.put(OrderStatus.CANCELLED,  Set.of());
    }

    // ── Dùng cho GHN webhook (bypass GHN_ONLY check) ─────────────
    public void validateTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        validateTransition(currentStatus, newStatus, true);
    }

    // ── Method chính ──────────────────────────────────────────────
    public void validateTransition(OrderStatus currentStatus, OrderStatus newStatus, boolean isGhnWebhook) {
        if (currentStatus == newStatus) return;

        // Staff không được tự set các status thuộc GHN flow
        if (GHN_ONLY_STATUSES.contains(newStatus) && !isGhnWebhook) {
            throw new InvalidStatusTransitionException(
                    String.format("Status %s can only be set by GHN webhook, not manually.", newStatus)
            );
        }

        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                    String.format("Cannot transition from %s to %s. Allowed: %s",
                            currentStatus, newStatus,
                            allowed.isEmpty() ? "none (terminal state)" : allowed)
            );
        }
    }

    // ── Lấy danh sách transition hợp lệ (dùng cho getAllowedNextStatuses) ──
    public Set<OrderStatus> getAllowedTransitions(OrderStatus status) {
        return Collections.unmodifiableSet(
                VALID_TRANSITIONS.getOrDefault(status, Set.of())
        );
    }

    public boolean isTerminal(OrderStatus status) {
        return VALID_TRANSITIONS.getOrDefault(status, Set.of()).isEmpty();
    }
}
