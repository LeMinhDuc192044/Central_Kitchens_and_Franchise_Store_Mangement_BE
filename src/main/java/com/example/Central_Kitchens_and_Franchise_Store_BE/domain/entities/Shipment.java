package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.RequiredNote;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.ShipServiceType;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.GhnItem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "shipment")
public class Shipment {

    @Id
    @Column(name = "shipment_code_id")
    private String shipmentCodeId;

    @Column(name = "note")
    private String note;

    @Column(name = "Ship Status")
    String shipStatus;

    @Column(name = "required_note")
    @Enumerated(EnumType.STRING)
    private RequiredNote required_note;

    @Column(name = "receiver")
    private String to_name;

    @Column(name = "receiver_phone_number")
    private String to_phone;

    @Column(name = "address")
    private String to_address;

    @Column(name = "ward_code")
    private String to_ward_code;

    @Column(name = "district_id")
    private Integer to_district_id;

    @Column(name = "cod_amount")
    private Integer cod_amount;

    @Column(name = "weight")
    private Integer weight;

    @Column(name = "length")
    private Integer length;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "service_type")
    @Enumerated(EnumType.STRING)
    private ShipServiceType service_type;

    @Column(name = "client_order_code")
    private String client_order_code;

    @Column(name = "ghn_order_code")
    private String ghnOrderCode;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Transient // Not stored in DB, used for GHN API
    private List<GhnItem> items;

    @OneToOne
    @JoinColumn(name = "order_detail_id_fk", referencedColumnName = "order_detail_id", insertable = false, updatable = false)
    private OrderDetail orderDetail;

    @OneToOne(mappedBy = "shipment", cascade = CascadeType.ALL)
    private ShipInvoice shipInvoice;
}