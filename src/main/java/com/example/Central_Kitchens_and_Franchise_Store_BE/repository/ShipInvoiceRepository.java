package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.ShipInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipInvoiceRepository extends JpaRepository<ShipInvoice, String> {
    Optional<ShipInvoice> findByShipmentCodeId(String shipmentCodeId);
}
