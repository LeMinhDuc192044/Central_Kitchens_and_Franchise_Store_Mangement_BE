//package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;
//
//
//import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.GhnWebhookPayload;
//import com.example.Central_Kitchens_and_Franchise_Store_BE.service.GhnWebhookService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/webhook")
//@RequiredArgsConstructor
//@Slf4j
//public class GhnWebhookController {
//
//    private final GhnWebhookService ghnWebhookService;
//
//    @PostMapping("/ghn")
//    public ResponseEntity<String> handleGhnWebhook(
//            @RequestBody GhnWebhookPayload payload) {
//
//        log.info("GHN Webhook hit: {}", payload);
//
//        try {
//            ghnWebhookService.handleWebhook(payload);
//            return ResponseEntity.ok("OK");         // GHN expects "OK" response
//        } catch (Exception e) {
//            log.error("Webhook processing failed: {}", e.getMessage());
//            return ResponseEntity.ok("OK");         // still return OK so GHN doesn't retry endlessly
//        }
//    }
//
//
//}
