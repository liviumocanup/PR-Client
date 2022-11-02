package com.utm.clientservice.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ClientSubOrderResponse {

    @JsonAlias("restaurant_id")
    private Integer restaurantId;

    @JsonAlias("order_id")
    private Integer orderId;

    @JsonAlias("estimated_waiting_time")
    private Double estimatedWaitingTime;

    @JsonAlias("created_time")
    private Long createdTime;

    @JsonAlias("registered_time")
    private Long registeredTime;
}
