package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "shipment")
public class Shipment {

    @Id
    @Column(name = "shipment_code_id")
    private String shipmentCodeId;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "place_of_receipt")
    private String placeOfReceipt;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "payment")
    private String payment;

    @Column(name = "name_of_consignee")
    private String nameOfConsignee;

    @Column(name = "phone_number_of_consignee")
    private String phoneNumberOfConsignee;

    @Column(name = "notes")
    private String notes;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "type_of_unit_load")
    private String typeOfUnitLoad;

    @Column(name = "name_of_receiver")
    private String nameOfReceiver;

    @Column(name = "phone_number_of_receiver")
    private String phoneNumberOfReceiver;

    @Column(name = "ship_status")
    private String shipStatus;

    @Column(name = "order_detail_id_fk")
    private String orderDetailId;

    @OneToOne
    @JoinColumn(name = "order_detail_id_fk", referencedColumnName = "order_detail_id", insertable = false, updatable = false)
    private OrderDetail orderDetail;

    @OneToOne(mappedBy = "shipment", cascade = CascadeType.ALL)
    private ShipInvoice shipInvoice;
}
