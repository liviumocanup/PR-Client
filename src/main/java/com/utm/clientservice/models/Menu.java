package com.utm.clientservice.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Menu {
    private Integer restaurants;

    @JsonAlias("restaurants_data")
    private List<Restaurant> restaurantsData;
}
