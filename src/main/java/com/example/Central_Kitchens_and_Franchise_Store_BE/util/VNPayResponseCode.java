package com.example.Central_Kitchens_and_Franchise_Store_BE.util;

import java.util.HashMap;
import java.util.Map;

public class VNPayResponseCode {

    private static final Map<String, String> CODES = new HashMap<>();

    static {
        CODES.put("00", "Giao dịch thành công");
        CODES.put("07", "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)");
        CODES.put("09", "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng");
        CODES.put("10", "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần");
        CODES.put("11", "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch");
        CODES.put("12", "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa");
        CODES.put("13", "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP)");
        CODES.put("24", "Giao dịch không thành công do: Khách hàng hủy giao dịch");
        CODES.put("51", "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch");
        CODES.put("65", "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày");
        CODES.put("75", "Ngân hàng thanh toán đang bảo trì");
        CODES.put("79", "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định");
        CODES.put("99", "Các lỗi khác (lỗi còn lại, không có trong danh sách mã lỗi đã liệt kê)");
    }

    public static String getMessage(String code) {
        return CODES.getOrDefault(code, "Lỗi không xác định - Mã: " + code);
    }

    public static boolean isSuccess(String code) {
        return "00".equals(code);
    }
}
