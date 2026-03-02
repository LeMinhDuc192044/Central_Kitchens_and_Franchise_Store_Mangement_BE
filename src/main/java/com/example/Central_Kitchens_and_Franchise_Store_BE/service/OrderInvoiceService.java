package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderInvoiceResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.OrderInvoicePaymentRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.OrderInvoice;
import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom.ResourceNotFoundException;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.FranchiseStorePaymentMethodRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderInvoiceRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderInvoiceService {

    private final OrderInvoiceRepository orderInvoiceRepository;
    private final OrderRepository orderRepository;
    private final FranchiseStorePaymentMethodRepository paymentMethodRepository;

    public OrderInvoiceResponse getInvoiceByOrderId(String orderId) {
        OrderInvoice invoice = orderInvoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for orderId: " + orderId));
        return mapToResponse(invoice);
    }

    @Transactional
    public OrderInvoiceResponse payInvoice(String orderId, OrderInvoicePaymentRequest request) {
        OrderInvoice invoice = orderInvoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for orderId: " + orderId));

        if ("PAID".equals(invoice.getInvoiceStatus())) {
            throw new IllegalStateException("Invoice is already paid for orderId: " + orderId);
        }

        String storeId = findStoreIdByOrderId(orderId);

        boolean validPaymentMethod = paymentMethodRepository
                .existsByStoreIdAndPaymentMethod(storeId, request.getPaymentType());
        if (!validPaymentMethod) {
            throw new IllegalArgumentException("Payment method '" + request.getPaymentType()
                    + "' is not supported by this store");
        }

        invoice.setPaymentType(request.getPaymentType());
        invoice.setTotalAmount(request.getTotalAmount());
        invoice.setInvoiceStatus("PAID");
        invoice.setPaidDate(LocalDate.now());

        OrderInvoice saved = orderInvoiceRepository.save(invoice);
        log.info("Invoice paid successfully for orderId: {}, storeId: {}", orderId, storeId);

        return mapToResponse(saved);
    }

    private String findStoreIdByOrderId(String orderId) {
        return orderRepository.findStoreIdByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or has no store: " + orderId));
    }

    private OrderInvoiceResponse mapToResponse(OrderInvoice invoice) {
        // Tìm orderId thật qua orderDetailId
        String realOrderId = orderRepository.findOrderIdByOrderDetailId(invoice.getOrderId());

        return OrderInvoiceResponse.builder()
                .orderInvoiceId(invoice.getOrderInvoiceId())
                .invoiceStatus(invoice.getInvoiceStatus())
                .paymentType(invoice.getPaymentType())
                .totalAmount(invoice.getTotalAmount())
                .paidDate(invoice.getPaidDate())
                .orderId(realOrderId)
                .build();
    }
}