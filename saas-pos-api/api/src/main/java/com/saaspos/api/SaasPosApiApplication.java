package com.saaspos.api;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class SaasPosApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SaasPosApiApplication.class, args);
	}
    @PostConstruct
    public void init() {
        // Forzar la zona horaria a Chile para toda la aplicación
        TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"));
        System.out.println("--- ZONA HORARIA CONFIGURADA: " + TimeZone.getDefault().getID() + " ---");
    }

}
