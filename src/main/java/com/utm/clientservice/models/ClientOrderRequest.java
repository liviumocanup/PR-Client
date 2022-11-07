package com.utm.clientservice.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ClientOrderRequest {
    @JsonAlias("client_id")
    private Integer clientId;

    private List<ClientSubOrderRequest> clientSubOrderRequests;
}
