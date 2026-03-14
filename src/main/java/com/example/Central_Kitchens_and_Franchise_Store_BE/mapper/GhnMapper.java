package com.example.Central_Kitchens_and_Franchise_Store_BE.mapper;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.ShipInvoiceResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.CentralFoods;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.ShipInvoice;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Shipment;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.ShipPaymentType;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.ShipServiceType;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.GhnItem;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.ShipInvoiceInfo;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.ShipmentInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GhnMapper {

    public List<GhnItem> convertToGhnItems(
            Map<String, Integer> foods,
            List<CentralFoods> centralFoodsList) {

        Map<String, CentralFoods> foodMap = centralFoodsList.stream()
                .collect(Collectors.toMap(CentralFoods::getCentralFoodId, f -> f));

        return foods.entrySet().stream()
                .map(entry -> {
                    CentralFoods food = foodMap.get(entry.getKey());

                    if (food == null) {
                        throw new RuntimeException("Food not found: " + entry.getKey());
                    }

                    return GhnItem.builder()
                            .name(food.getFoodName())
                            .code(food.getCentralFoodId())
                            .price(food.getUnitPriceFood())
                            .quantity(entry.getValue())
                            .weight(food.getWeight())
                            .length(food.getLength())
                            .width(food.getWidth())
                            .height(food.getHeight())
                            .build();
                })
                .toList();
    }

    public ShipmentInfo convertToDTO(Shipment shipment) {
        if (shipment == null) {
            return null;
        }

        Integer serviceId;
        if(shipment.getService_type().equals(ShipServiceType.EXPRESS)) {
            serviceId = 1;
        } else if(shipment.getService_type().equals(ShipServiceType.STANDARD)) {
            serviceId = 2;
        } else {
            throw new IllegalStateException("Shipment must be EXPRESS or EXPRESS only!!!!");
        }



        ShipmentInfo response = ShipmentInfo.builder()
                .shipmentCodeId(shipment.getShipmentCodeId())
                .note(shipment.getNote())
                .requiredNote(shipment.getRequired_note())
                .toName(shipment.getTo_name())
                .toPhone(shipment.getTo_phone())
                .toAddress(shipment.getTo_address())
                .toWardCode(shipment.getTo_ward_code())
                .toDistrictId(shipment.getTo_district_id())
                .codAmount(shipment.getCod_amount())
                .weight(shipment.getWeight())
                .length(shipment.getLength())
                .width(shipment.getWidth())
                .height(shipment.getHeight())
                .serviceTypeId(serviceId)
                .clientOrderCode(shipment.getClient_order_code())
                .ghnOrderCode(shipment.getGhnOrderCode())
                .orderDetailId(shipment.getOrderDetail().getOrderDetailId())
                .build();

        // Map ShipInvoice if it exists
        if (shipment.getShipInvoice() != null) {
            response.setShipInvoice(convertShipInvoiceToInfo(shipment.getShipInvoice()));
        }

        return response;
    }

    /**
     * Convert ShipInvoice to nested DTO
     */
    private ShipInvoiceInfo convertShipInvoiceToInfo(ShipInvoice shipInvoice) {
        if (shipInvoice == null) {
            return null;
        }

        Integer paymentId;
        if(shipInvoice.getPaymentType().equals(ShipPaymentType.SENDER_PAY)) {
            paymentId = 1;
        } else if(shipInvoice.getPaymentType().equals(ShipPaymentType.RECEIVER_PAY)) {
            paymentId = 2;
        } else {
            throw new IllegalStateException("shipInvoice must be RECEIVER_PAY or SENDER_PAY only!!!!");
        }

        String paymentTypeName = (shipInvoice.getPaymentType().toString());




        return  ShipInvoiceInfo.builder()
                .shipInvoiceId(shipInvoice.getShipInvoiceId())
                .totalPrice(shipInvoice.getTotalPrice())
                .paymentTypeId(paymentId)
                .paymentTypeName(paymentTypeName)
                .build();
    }



    /**
     * Convert list of Shipment entities to list of DTOs
     */
    public List<ShipmentInfo> convertToDTOList(List<Shipment> shipments) {
        if (shipments == null) {
            return null;
        }

        return shipments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ShipInvoiceResponse toShipInvoiceResponse(ShipInvoice invoice) {
        return ShipInvoiceResponse.builder()
                .shipInvoiceId(invoice.getShipInvoiceId())
                .shipmentCodeId(invoice.getShipmentCodeId())
                .ghnOrderCode(invoice.getShipment() != null
                        ? invoice.getShipment().getGhnOrderCode() : null)
                .totalPrice(invoice.getTotalPrice())
                .paymentType(invoice.getPaymentType())
                .invoiceStatus(invoice.getInvoiceStatus())
                .paidAt(invoice.getPaidAt())
                .build();
    }

}
