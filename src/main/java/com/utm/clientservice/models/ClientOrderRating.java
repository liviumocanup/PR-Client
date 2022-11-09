package com.utm.clientservice.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ClientOrderRating {
    @JsonAlias("client_id")
    private Integer clientId;

    @JsonAlias("order_id")
    private Integer orderId;

    private List<ClientSubOrderRatingRequest> orders;
}
