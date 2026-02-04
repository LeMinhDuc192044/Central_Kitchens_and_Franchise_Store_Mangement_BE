package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Tag(name = "Swagger Test", description = "Endpoints to verify Swagger works")
public class TestController {
    @Operation(summary = "Test Swagger API")
    @GetMapping("/hello")
    public String hello() {
        return "Swagger is working!";
    }
}
