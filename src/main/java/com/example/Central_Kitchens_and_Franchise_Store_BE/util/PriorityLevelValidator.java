package com.example.Central_Kitchens_and_Franchise_Store_BE.util;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Slf4j
@Component
public class PriorityLevelValidator {

    private static final int MIN_PRIORITY = 1;  // Mức độ Cao nhất
    private static final int MAX_PRIORITY = 3;  // Mức độ Thấp nhất

    // Các trạng thái không được phép thay đổi priority
    private static final Set<OrderStatus> LOCKED_STATUSES = EnumSet.of(
            OrderStatus.CANCELLED,
            OrderStatus.COOKING_DONE
    );

    public void validatePriorityChange(OrderStatus currentStatus,
                                       Integer currentPriority,
                                       Integer newPriority) {

        // Rule 1: Kiểm tra priority mới nằm trong khoảng hợp lệ
        if (!isValidPriority(newPriority)) {
            log.warn("Invalid priority level: {}. Must be between {} and {}",
                    newPriority, MIN_PRIORITY, MAX_PRIORITY);
            throw new IllegalArgumentException(
                    String.format("Priority level must be between %d and %d", MIN_PRIORITY, MAX_PRIORITY)
            );
        }

        // Rule 2: Không được đổi nếu order đã cancelled hoặc cooking done
        if (LOCKED_STATUSES.contains(currentStatus)) {
            log.warn("Cannot change priority for order with status: {}", currentStatus);
            throw new IllegalStateException(
                    String.format("Cannot change priority for order with status: %s", currentStatus)
            );
        }

        // Rule 3: Nếu currentPriority không null, priority mới phải khác priority cũ
        if (currentPriority != null && currentPriority.equals(newPriority)) {
            log.warn("New priority is same as current priority: {}", currentPriority);
            throw new IllegalArgumentException(
                    String.format("New priority (%d) is same as current priority", newPriority)
            );
        }
    }

    /**
     * Kiểm tra xem priority có hợp lệ không (1-3)
     */
    public boolean isValidPriority(Integer priority) {
        return priority != null && priority >= MIN_PRIORITY && priority <= MAX_PRIORITY;
    }

    public boolean shouldAutoTransitionToInProgress(OrderStatus currentStatus,Integer currentPriority, Integer newPriority) {
        boolean isFirstTimeAssign = (currentPriority == null && newPriority != null);
        boolean isPending = (currentStatus == OrderStatus.PENDING);

        return isFirstTimeAssign && isPending;
    }


    public String getPriorityName(Integer priority) {
        if (priority == null) return "NOT_SET";
        return switch (priority) {
            case 1 -> "HIGH";       // Cao nhất
            case 2 -> "MEDIUM";     // Trung bình
            case 3 -> "LOW";        // Thấp nhất
            default -> "UNKNOWN";
        };
    }


}