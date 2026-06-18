package com.distribuidos.bully;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicación principal del Algoritmo Bully para elección de coordinador.
 * 
 * Cada instancia de esta aplicación representa un nodo (proceso) en el
 * sistema distribuido. Los nodos se comunican mediante REST HTTP.
 * 
 * @author Práctica 10 - Sistemas Distribuidos
 */
@SpringBootApplication
@EnableScheduling
public class BullyAlgorithmApplication {

    public static void main(String[] args) {
        SpringApplication.run(BullyAlgorithmApplication.class, args);
    }
}
