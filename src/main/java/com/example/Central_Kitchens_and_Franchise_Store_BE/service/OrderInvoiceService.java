package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderInvoiceResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.OrderInvoice;
import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom.ResourceNotFoundException;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.FranchiseStorePaymentMethodRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderInvoiceRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;



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