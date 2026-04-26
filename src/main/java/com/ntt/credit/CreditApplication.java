package com.ntt.credit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class CreditApplication {
  public static void main(String[] args) {
    SpringApplication.run(CreditApplication.class, args);
  }
}
