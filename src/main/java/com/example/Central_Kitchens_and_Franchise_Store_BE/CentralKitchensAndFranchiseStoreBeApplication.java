package com.example.Central_Kitchens_and_Franchise_Store_BE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class CentralKitchensAndFranchiseStoreBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CentralKitchensAndFranchiseStoreBeApplication.class, args);
	}

}
