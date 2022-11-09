package com.utm.clientservice.models;

import com.utm.clientservice.service.ClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.utm.clientservice.service.ClientService.TIME_UNIT;

@Slf4j
public class Client {
    public final static String CHECK_ORDER = "/v2/order/";
    public final static String GET_ALL_MENUS = "/menu";
    public final static String ORDER = "/order";
    public final static String RATING = "/rating";


    private final Integer id;
    private static final AtomicInteger atomicInteger = new AtomicInteger();

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final Random random = new Random();

    private ClientOrderResponse clientOrderResponse;

    private final List<ClientSubOrderRatingRequest> subOrderRatingRequests = new ArrayList<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public Client() {
        this.id = atomicInteger.incrementAndGet();
    }

    public void init() {
        executor.submit(() -> {
            Menu menu = requestMenu();

            ClientOrderRequest clientOrderRequest = generateOrder(menu);
            clientOrderResponse = submitOrderToFoodOrderingService(clientOrderRequest);
            log.info("<----| Order sent : " + clientOrderRequest + ", with response: " + clientOrderResponse);

            for (ClientSubOrderResponse clientSubOrderResponse : clientOrderResponse.getClientSubOrderResponses()) {
                String restaurantUrl = menu.getRestaurantsData().stream()
                        .filter(r -> r.getRestaurantId().equals(clientSubOrderResponse.getRestaurantId()))
                        .map(Restaurant::getAddress)
                        .findAny()
                        .orElseThrow();

                ClientSubOrderRatingRequest subOrderRatingRequest = new ClientSubOrderRatingRequest();
                subOrderRatingRequest.setOrderId(clientSubOrderResponse.getOrderId());
                subOrderRatingRequest.setRestaurantId(clientSubOrderResponse.getRestaurantId());
                subOrderRatingRequest.setEstimatedWaitingTime(clientSubOrderResponse.getEstimatedWaitingTime());
                subOrderRatingRequest.setWaitingTime(clientSubOrderResponse.getEstimatedWaitingTime());

                executor.schedule(() -> checkIfSubOrderIsReady(subOrderRatingRequest, restaurantUrl), clientSubOrderResponse.getEstimatedWaitingTime().longValue() * TIME_UNIT + 5, TimeUnit.MILLISECONDS);
            }
        });
    }

    private void checkIfSubOrderIsReady(ClientSubOrderRatingRequest subOrderRatingRequest, String restaurantUrl) {
        //log.info("Order status for suborder " + clientSubOrderResponse);
        ResponseEntity<ClientOrder> response = restTemplate.getForEntity(restaurantUrl + CHECK_ORDER + subOrderRatingRequest.getOrderId(), ClientOrder.class);

        ClientOrder clientOrder = response.getBody();
        //log.info("Order status : " + clientOrder);

        if (clientOrder.getIsReady()) {
            log.info("|----> Order is ready : " + clientOrder);
            subOrderRatingRequest.setRating(getRatingForSuborder(clientOrder));
            subOrderRatingRequests.add(subOrderRatingRequest);
            if (clientOrderResponse.getClientSubOrderResponses().size() == subOrderRatingRequests.size()) {
                ClientOrderRating orderRating = new ClientOrderRating();
                orderRating.setClientId(id);
                orderRating.setOrders(subOrderRatingRequests);
                orderRating.setOrderId(clientOrderResponse.getId());
                restTemplate.postForEntity(ClientService.FOOD_ORDER_SERVICE_URL + RATING, orderRating, Void.class);
                new Client().init();
            }
        } else {
            subOrderRatingRequest.setEstimatedWaitingTime(subOrderRatingRequest.getEstimatedWaitingTime() + clientOrder.getEstimatedWaitingTime());
            executor.schedule(() -> checkIfSubOrderIsReady(subOrderRatingRequest, restaurantUrl), clientOrder.getEstimatedWaitingTime().longValue() * TIME_UNIT + 5, TimeUnit.MILLISECONDS);
        }
    }

    public Integer getRatingForSuborder(ClientOrder clientOrder) {
        long prepTime = clientOrder.getPreparedTime() - clientOrder.getCreatedTime();
        double maxWaitTime = clientOrder.getMaximumWaitTime() * TIME_UNIT;
        int rating;

        if (prepTime < maxWaitTime) {
            rating = 5;
        } else if (prepTime < maxWaitTime * 1.1) {
            rating = 4;
        } else if (prepTime < maxWaitTime * 1.2) {
            rating = 3;
        } else if (prepTime < maxWaitTime * 1.3) {
            rating = 2;
        } else if (prepTime < maxWaitTime * 1.4) {
            rating = 1;
        } else {
            rating = 0;
        }

        log.info("ID: " + clientOrder.getOrderId() + " has rating: " + rating + ", with maxWaitTime: " + maxWaitTime + ", and preparationTime: " + prepTime);
        return rating;
    }

    private ClientOrderResponse submitOrderToFoodOrderingService(ClientOrderRequest clientOrderRequest) {
        ResponseEntity<ClientOrderResponse> response = restTemplate.postForEntity(ClientService.FOOD_ORDER_SERVICE_URL + ORDER, clientOrderRequest, ClientOrderResponse.class);
        return response.getBody();
    }

    private Menu requestMenu() {
        ResponseEntity<Menu> response = restTemplate.getForEntity(ClientService.FOOD_ORDER_SERVICE_URL + GET_ALL_MENUS, Menu.class);
        return response.getBody();
    }

    private ClientOrderRequest generateOrder(Menu menu) {
        ClientOrderRequest clientOrderRequest = new ClientOrderRequest();

        int totalRestaurants = Math.max(1, random.nextInt(menu.getRestaurants()));
        List<Restaurant> restaurantList = new ArrayList<>(menu.getRestaurantsData());

        List<ClientSubOrderRequest> clientSubOrderRequests = new ArrayList<>();

        for (int i = 0; i < totalRestaurants; i++) {
            ClientSubOrderRequest clientSubOrderRequest = new ClientSubOrderRequest();
            clientSubOrderRequests.add(clientSubOrderRequest);

            int randomRestaurantIndex = random.nextInt(menu.getRestaurants());
            Restaurant restaurant = restaurantList.get(randomRestaurantIndex);
            restaurantList.remove(restaurant);

            int numberOfItems = random.nextInt(5) + 1;

            int priority;
            if (numberOfItems < 2)
                priority = 5;
            else if (numberOfItems < 3)
                priority = 4;
            else if (numberOfItems < 4)
                priority = 3;
            else if (numberOfItems < 5)
                priority = 2;
            else priority = 1;

            long maxPrepTime = -1;

            for (int j = 0; j < numberOfItems; j++) {
                int itemIndex = random.nextInt(restaurant.getMenuItems());
                Food menuItem = restaurant.getMenu().get(itemIndex);

                clientSubOrderRequest.getItems().add(menuItem.getId());
                maxPrepTime = Math.max(maxPrepTime, menuItem.getPreparationTime());
            }
            clientSubOrderRequest.setPriority(0);
            clientSubOrderRequest.setMaximumWaitTime(maxPrepTime * 1.8);
            clientSubOrderRequest.setRestaurantId(restaurant.getRestaurantId());
            clientSubOrderRequest.setPriority(priority);
        }
        clientOrderRequest.setClientSubOrderRequests(clientSubOrderRequests);
        clientOrderRequest.setClientId(id);

        return clientOrderRequest;
    }
}
