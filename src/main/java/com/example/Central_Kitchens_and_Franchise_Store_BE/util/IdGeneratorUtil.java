package com.example.Central_Kitchens_and_Franchise_Store_BE.util;

import java.time.LocalDate;
import java.util.UUID;

public class IdGeneratorUtil {

    /**
     * Generate Order ID với format: ORD-YYYYMMDD-XXXX
     * Ví dụ: ORD-20260205-A1B2C3D4
     * @return Order ID
     */
    public static String generateOrderId() {
        String dateStr = LocalDate.now().toString().replace("-", "");
        String randomStr = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("ORD-%s-%s", dateStr, randomStr);
    }

    /**
     * Generate OrderDetail ID với format: OD-XXXXXXXXXXXX
     * Ví dụ: OD-A1B2C3D4E5F6
     * @return OrderDetail ID
     */
    public static String generateOrderDetailId() {
        String randomStr = UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        return "OD-" + randomStr;
    }

    /**
     * Generate OrderDetailItem ID với format: ODI-XXXXXXXXXX
     * Ví dụ: ODI-A1B2C3D4E5
     * @return OrderDetailItem ID
     */
    public static String generateOrderDetailItemId() {
        String randomStr = UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        return "ODI-" + randomStr;
    }

    /**
     * Generate Order ID với custom date
     * @param date ngày tạo order
     * @return Order ID
     */
    public static String generateOrderId(LocalDate date) {
        String dateStr = date.toString().replace("-", "");
        String randomStr = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("ORD-%s-%s", dateStr, randomStr);
    }
}