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

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public Client() {
        this.id = atomicInteger.incrementAndGet();
    }

    public void init() {
        executor.submit(() -> {
            Menu menu = requestMenu();

            ClientOrderRequest clientOrderRequest = generateOrder(menu);
            clientOrderResponse = submitOrderToFoodOrderingService(clientOrderRequest);
            log.info("<----| Order sent : " + clientOrderResponse);

            for (ClientSubOrderResponse clientSubOrderResponse : clientOrderResponse.getClientSubOrderResponses()) {
                String restaurantUrl = menu.getRestaurantsData().stream()
                        .filter(r -> r.getRestaurantId().equals(clientSubOrderResponse.getRestaurantId()))
                        .map(Restaurant::getAddress)
                        .findAny()
                        .orElseThrow();

                executor.schedule(() -> checkIfSubOrderIsReady(clientSubOrderResponse, restaurantUrl), clientSubOrderResponse.getEstimatedWaitingTime().longValue() * TIME_UNIT + 5, TimeUnit.MILLISECONDS);
            }
        });
    }

    private void checkIfSubOrderIsReady(ClientSubOrderResponse clientSubOrderResponse, String restaurantUrl) {
        //log.info("Order status for suborder " + clientSubOrderResponse);
        ResponseEntity<ClientOrder> response = restTemplate.getForEntity(restaurantUrl + CHECK_ORDER + clientSubOrderResponse.getOrderId(), ClientOrder.class);

        ClientOrder clientOrder = response.getBody();
        //log.info("Order status : " + clientOrder);

        if (clientOrder.getIsReady()) {
            log.info("|----> Order is ready : " + clientOrder);
            //request Rating
            new Client().init();
        } else {
            clientSubOrderResponse.setEstimatedWaitingTime(clientSubOrderResponse.getEstimatedWaitingTime() + clientOrder.getEstimatedWaitingTime());
            executor.schedule(() -> checkIfSubOrderIsReady(clientSubOrderResponse, restaurantUrl), clientOrder.getEstimatedWaitingTime().longValue() * TIME_UNIT + 5, TimeUnit.MILLISECONDS);
        }
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

            int numberOfItems = random.nextInt(2) + 1;

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
        }
        clientOrderRequest.setClientSubOrderRequests(clientSubOrderRequests);
        clientOrderRequest.setClientId(id);

        return clientOrderRequest;
    }
}
