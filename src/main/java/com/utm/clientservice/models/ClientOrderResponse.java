package com.utm.clientservice.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ClientOrderResponse {
    @JsonAlias("order_id")
    private Integer id;

    @JsonAlias("orders")
    private List<ClientSubOrderResponse> clientSubOrderResponses;
}
