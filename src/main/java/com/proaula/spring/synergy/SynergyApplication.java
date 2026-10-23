package com.proaula.spring.synergy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling // recálculo nocturno del riesgo de los proyectos
@EntityScan("com.proaula.spring.synergy.Model")  
public class SynergyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynergyApplication.class, args);
    }
}