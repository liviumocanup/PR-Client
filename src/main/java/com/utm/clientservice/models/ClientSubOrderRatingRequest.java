package com.utm.clientservice.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ClientSubOrderRatingRequest {
    @JsonAlias("restaurant_id")
    private Integer restaurantId;

    @JsonAlias("order_id")
    private Integer orderId;

    private Integer rating;

    @JsonAlias("estimated_waiting_time")
    private Double estimatedWaitingTime;

    @JsonAlias("waiting_time")
    private Double waitingTime;
}
