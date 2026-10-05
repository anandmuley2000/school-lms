package com.schoollms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SchoolLmsApplication {
  public static void main(String[] args) {
    SpringApplication.run(SchoolLmsApplication.class, args);
  }
}
