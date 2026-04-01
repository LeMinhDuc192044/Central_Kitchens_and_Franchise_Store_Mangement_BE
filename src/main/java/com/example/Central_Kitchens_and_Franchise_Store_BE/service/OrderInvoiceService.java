package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderInvoiceResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.OrderInvoice;
import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom.ResourceNotFoundException;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderInvoiceRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.PaymentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderInvoiceService {

    private final OrderInvoiceRepository orderInvoiceRepository;
    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    public OrderInvoiceResponse getInvoiceByOrderId(String orderId) {
        OrderInvoice invoice = orderInvoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for orderId: " + orderId));
        return mapToResponse(invoice);
    }

    public List<OrderInvoiceResponse> getAllInvoice() {
        return orderInvoiceRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private OrderInvoiceResponse mapToResponse(OrderInvoice invoice) {



        return OrderInvoiceResponse.builder()
                .orderInvoiceId(invoice.getOrderInvoiceId())
                .orderId(invoice.getOrderId())
                .invoiceStatus(String.valueOf(invoice.getInvoiceStatus()))
                .paymentType(invoice.getPaymentType())
                .totalAmount(invoice.getTotalAmount())
                .paidDate(invoice.getPaidDate())
                .build();
    }
}