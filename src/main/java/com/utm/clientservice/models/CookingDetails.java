package com.utm.clientservice.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CookingDetails {
    @JsonAlias("food_id")
    private int foodId;

    @JsonAlias("cook_id")
    private int cookId;

    @Override
    public String toString() {
        return "CookingDetails{" +
                "foodId=" + foodId +
                ", cookId=" + cookId +
                '}';
    }
}
