package com.example.FakeErrorSimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FakeErrorSimulatorApplication {

	public static void main(String[] args) {

        SpringApplication.run(FakeErrorSimulatorApplication.class, args);
        System.out.println("Fake Error Simulator Application is running...");
	}

}
