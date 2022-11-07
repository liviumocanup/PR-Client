package com.utm.clientservice.service;

import com.utm.clientservice.models.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class ClientService {
    public static final Long TIME_UNIT = 50L;
    private static final Integer NUMBER_OF_CLIENTS = 3;
    public static String FOOD_ORDER_SERVICE_URL;

    @Value("${food-order-service.url}")
    public void setFoodOrderServiceUrl(String url) {
        FOOD_ORDER_SERVICE_URL = url;
        initClients();
    }


    public ClientService() {
    }

    public void initClients() {
        System.out.println(FOOD_ORDER_SERVICE_URL);
        for (int i = 0; i < NUMBER_OF_CLIENTS; i++) {
            new Client().init();
        }
    }
}
