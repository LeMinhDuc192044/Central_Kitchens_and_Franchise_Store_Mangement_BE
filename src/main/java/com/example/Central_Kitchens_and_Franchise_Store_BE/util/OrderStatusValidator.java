package com.example.Central_Kitchens_and_Franchise_Store_BE.util;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class OrderStatusValidator {

    // Định nghĩa các transition hợp lệ
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = new HashMap<>();

    static {
        // PENDING có thể chuyển sang IN_PROGRESS hoặc CANCELLED
        VALID_TRANSITIONS.put(OrderStatus.PENDING,
                Set.of(OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED));

        // IN_PROGRESS có thể chuyển sang COOKING_DONE, WAITING_FOR_UPDATE hoặc CANCELLED
        VALID_TRANSITIONS.put(OrderStatus.IN_PROGRESS,
                Set.of(OrderStatus.COOKING_DONE, OrderStatus.WAITING_FOR_UPDATE, OrderStatus.CANCELLED));

        // WAITING_FOR_UPDATE có thể chuyển sang IN_PROGRESS hoặc CANCELLED
        // Lưu ý: KHÔNG thể chuyển trực tiếp sang COOKING_DONE
        VALID_TRANSITIONS.put(OrderStatus.WAITING_FOR_UPDATE,
                Set.of(OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED));

        // COOKING_DONE là trạng thái cuối (có thể không chuyển đi đâu)
        // hoặc chỉ có thể chuyển sang CANCELLED trong trường hợp đặc biệt
        VALID_TRANSITIONS.put(OrderStatus.COOKING_DONE,
                Set.of()); // Không thể chuyển sang trạng thái nào khác

        // CANCELLED là trạng thái cuối, không thể chuyển đi đâu
        VALID_TRANSITIONS.put(OrderStatus.CANCELLED,
                Set.of());
    }

    public void validateTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Nếu trạng thái không thay đổi thì không cần validate
        if (currentStatus == newStatus) {
            return;
        }

        Set<OrderStatus> allowedTransitions = VALID_TRANSITIONS.get(currentStatus);

        if (allowedTransitions == null || !allowedTransitions.contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                    String.format("Invalid status transition: Cannot change from %s to %s. Allowed transitions from %s: %s",
                            currentStatus, newStatus, currentStatus,
                            allowedTransitions != null ? allowedTransitions : "None")
            );
        }
    }


    public boolean isValidTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return true;
        }

        Set<OrderStatus> allowedTransitions = VALID_TRANSITIONS.get(currentStatus);
        return allowedTransitions != null && allowedTransitions.contains(newStatus);
    }


    public Set<OrderStatus> getAllowedTransitions(OrderStatus currentStatus) {
        return VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
    }

}
