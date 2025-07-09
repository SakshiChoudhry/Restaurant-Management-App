package com.restaurantapp;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Controller.*;
import com.restaurantapp.Repository.LocationRepository;
import com.restaurantapp.Service.LocationService;
import com.restaurantapp.Service.ReservationService;
import com.restaurantapp.di.DaggerAppComponent;
import com.restaurantapp.Middleware.AuthMiddleware;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main Lambda handler for API requests.
 */
@LambdaHandler(
		lambdaName = "api-handler",
		roleName = "api-handler-role",
		isPublishVersion = true,
		aliasName = "${lambda_alias_name}",
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key="order_table",value="${order_table}"),
		@EnvironmentVariable(key = "user_table", value = "${user_table}"),
		@EnvironmentVariable(key = "waiter_table", value = "${waiter_table}"),
		@EnvironmentVariable(key = "booking_table", value = "${booking_table}"),
		@EnvironmentVariable(key = "location_table", value = "${location_table}"),
		@EnvironmentVariable(key = "cognito_pool", value = "${cognito_pool}"),
		@EnvironmentVariable(key = "dishes_table", value = "${dishes_table}"),
		@EnvironmentVariable(key = "feedback_table", value = "${feedback_table}"),
		@EnvironmentVariable(key = "special_dishes_table", value = "${special_dishes_table}"),
		@EnvironmentVariable(key = "tables_table_name", value = "${tables_table_name}"),
		@EnvironmentVariable(key = "cart_table", value = "${cart_table}"),
		@EnvironmentVariable(key = "order_table", value = "${order_table}"),
		@EnvironmentVariable(key = "jwt_secret", value = "${jwt_secret}")
})
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
	private static final Logger LOG = LoggerFactory.getLogger(ApiHandler.class);
	private final DemoController demoController;
	private final UserController userController;
	private final ProfileController profileController;
	private final AuthMiddleware authMiddleware;
	private final BookingController bookingController;
	private final ReservationGetController reservationGetController;
	private final ReservationDeletionController reservationDeletionController;
	private final ObjectMapper objectMapper;
	private final ReservationService reservationService;
	private final ReservationController reservationController;
	private static final ObjectMapper objectMapper2 = new ObjectMapper();
	private final LocationController locationController;
	private final TableController tableController;
	private final DishController dishController;
	private final CartController cartController; // Added CartController
	private final AnonymousFeedbackController anonymousFeedbackController;

	private final ReservationWaiterController reservationWaiterController;
	//dishes and feedbacks
	private final DishesController dishesController;
	private final FeedbackController feedbackController;

	// US15
	private final CustomerFeedbackController customerFeedbackController;



	public ApiHandler() {
		// Initialize dependencies using Dagger
		var appComponent = DaggerAppComponent.create();
		this.demoController = appComponent.demoController();
		this.userController = appComponent.userController();
		this.reservationWaiterController=appComponent.reservationWaiterController();
		this.profileController = appComponent.profileController();
		this.bookingController = appComponent.bookingController();
		this.authMiddleware = appComponent.authMiddleware();
		this.reservationDeletionController = appComponent.reservationDeletionController(); // Add this line
		this.objectMapper = appComponent.objectMapper();
		this.reservationService = appComponent.reservationService();
		this.reservationController = appComponent.reservationController();
		this.tableController = appComponent.tableController();
		this.reservationGetController=appComponent.reservationGetController();
		this.dishController=appComponent.dishController();
		this.anonymousFeedbackController = appComponent.anonymousFeedbackController();

		// location and dishes
		LocationRepository repository=new LocationRepository();
		LocationService locationService=new LocationService(repository);
		this.locationController = new LocationController(locationService);

		this.dishesController = appComponent.dishesController();
		this.feedbackController=appComponent.feedbackController();
		this.cartController = appComponent.cartController(); // Initialize CartController

		//US15
		this.customerFeedbackController = appComponent.customerFeedbackController();


		LOG.info("ApiHandler initialized with controllers");
	}

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
		try {
			LOG.info("Request received: {} {}", request.getHttpMethod(), request.getPath());

			// Handle OPTIONS requests for CORS
			if ("OPTIONS".equals(request.getHttpMethod())) {
				return handleOptionsRequest();
			}

			// Extract path and normalize
			String path = normalizePath(request.getPath());
			LOG.info("Processing normalized path: {}", path);

			// Route request to appropriate handler
			return routeRequest(request, path);
		} catch (Exception e) {
			LOG.error("Unhandled error processing request", e);
			return ApiResponse.serverError("Internal server error: " + e.getMessage());
		}
	}

	private String normalizePath(String path) {
		if (path.startsWith("/api")) {
			return path.substring(4);
		} else if (path.startsWith("/admin")) {
			return path.substring(6);
		}
		return path;
	}

	private APIGatewayProxyResponseEvent routeRequest(APIGatewayProxyRequestEvent request, String path) {
		String method = request.getHttpMethod();
		String body = request.getBody();

		Map<String, String> queryParams = request.getQueryStringParameters();
		// Handle null queryParams (AWS can return null if no query parameters are present)
		if (queryParams == null) {
			queryParams = new HashMap<>();
		}

		// Public endpoints
		if ("/v1/hello".equals(path) && "GET".equals(method)) {
			String result = demoController.hello();
			return ApiResponse.success(Map.of("message", result));
		} else if ("/v1/auth/sign-up".equals(path) && "POST".equals(method)) {
			return userController.signUp(body);
		} else if ("/v1/auth/sign-in".equals(path) && "POST".equals(method)) {
			return userController.login(body);
		} 

		//dishes and feedbacks
		else if ("/v1/dishes/popular".equals(path)) {
			if("GET".equals(method))
				return dishesController.getPopularDishes();
		}
		else if (path.matches("/v1/locations/[^/]+/feedbacks") && "GET".equals(method)) {
			// Extract location ID from path
			String locationId = path.split("/")[3];
			return feedbackController.getFeedback(
					Map.of("id", locationId),
					request.getQueryStringParameters() != null ? request.getQueryStringParameters() : Collections.emptyMap()
			);}
		else if (path.equals("/v1/locations") && "GET".equals(request.getHttpMethod())) {
			return locationController.getAllLocations();
		}
		// Handle /locations/{id}/speciality-dishes endpoint
		else if (path.matches("^/v1/locations/[^/]+/speciality-dishes$") && "GET".equals(request.getHttpMethod())) {
			String locationId = path.split("/")[3];
			return locationController.getSpecialityDishes(locationId);
		}

		// us-5
		else if ("/v1/location/select-options".equals(path)) {
			if ("GET".equals(method)) {
				LOG.info("Route matched: /v1/location/select-options (GET)");
				return locationController.getLocations();
			} else {
				LOG.warn("Invalid method for /v1/location/select-options: {}", method);
				return ApiResponse.methodNotAllowed("Method Not Allowed: Use GET instead.");
			} }
		else if ("/v1/bookings/tables".equals(path) && "GET".equals(method)) {
			return tableController.handleTableReservations(request);
		}

		//US 11
		else if (path.equals("/v1/feedbacks/visitor") && method.equals("GET")) {
			String reservationId = queryParams.get("reservationId");
			String secretCode = queryParams.get("secretCode");
			return anonymousFeedbackController.authenticateFeedback(reservationId, secretCode);
		}

		
		
		//admin
		else if ("/v1/create-waiter".equals(path) && "POST".equals(method)) {
			return userController.createWaiter(body);
		} else if ("/v1/remove-waiter".equals(path) && "POST".equals(method)) {
			return userController.updateRole(body);
			
		}
		else if ("/v1/update-booking".equals(path) && "PUT".equals(method)) {
			return reservationWaiterController.updateReservationByWaiter(body);
		}
		else if ("/v1/dishes".equals(path) && "GET".equals(method)) {
			return dishController.getDishes(request.getQueryStringParameters());
		}
		else if (path.matches("/v1/dishes/[^/]+") && "GET".equals(method)) {
			String dishId = path.substring(path.lastIndexOf("/") + 1);
			return dishController.getDishById(dishId);
		}
//		else if ("/v1/reservations-by-waiter".equals(path) && "GET".equals(method)) {
//			return reservationGetController.getUserReservations("waiter2@gmail.com");
//		}



		// Protected endpoints
		else if ("/v1/auth/profile".equals(path) && "GET".equals(method)) {
			return handleProtectedEndpoint(request, claims -> profileController.getProfile(claims));
		}else if ("/v1/bookings/client".equals(path) && "POST".equals(method)) {
			return handleProtectedEndpoint(request, claims -> bookingController.createBooking(body, claims));
		}else if (path.matches("/v1/bookings/client/[^/]+") && "PUT".equals(method)) {
			String reservationId = path.split("/")[4];
			return handleProtectedEndpoint(request, claims -> bookingController.updateBooking(reservationId, body, claims));
		}else if ("/v1/reservations".equals(path) && "GET".equals(method)) {
			return handleProtectedEndpoint(request, claims -> reservationController.getUserReservations(claims));
		}else if (("/v1/feedbacks".equals(path) && "POST".equals(method)) || ("/v1/feedbacks/".equals(path) && "POST".equals(method))) {
			return handleProtectedEndpoint(request, claims -> customerFeedbackController.createFeedback(body, claims));
		}else if ("/v1/feedbacks/update".equals(path) && "PUT".equals(method)) {
			return handleProtectedEndpoint(request, claims -> customerFeedbackController.updateFeedback(body, claims));
		}else if (path.matches("/v1/feedbacks/[^/]+") && "GET".equals(method)) {
			String reservationId = path.split("/")[3];
			return handleProtectedEndpoint(request, claims -> customerFeedbackController.getFeedback(reservationId, claims));
		}

		//us10Sakshi
		// In your routeRequest method
		else if (path.matches("/v1/reservations/[^/]+/available-dishes") && "GET".equals(method)) {
			String reservationId = path.split("/")[3];
			return dishesController.getAvailableDishesForReservation(reservationId);
		}
		else if (path.matches("/v1/reservations/[^/]+/order/[^/]+") && "POST".equals(method)) {
			String[] pathParts = path.split("/");
			String reservationId = pathParts[3];
			String dishId = pathParts[5];
			return handleProtectedEndpoint(request, claims ->
					cartController.addDishToCart(reservationId, dishId, claims));
		}
		else if ("/v1/cart".equals(path) && "GET".equals(method)) {
			return handleProtectedEndpoint(request, claims ->
					cartController.getCart(claims));
		}
		else if ("/v1/reservations-by-waiter".equals(path) && "GET".equals(method)) {
			return handleProtectedEndpoint(request, claims -> reservationGetController.getUserReservations(claims));
		}
		else if ("/v1/bookings/waiter".equals(path) && "POST".equals(method)) {
			return handleProtectedEndpoint(request, claims -> reservationWaiterController.createReservationByWaiter(body,claims));
		}
		// In your ApiHandler.java, make sure the PUT /v1/cart route is defined:
		// In your ApiHandler.java, make sure the PUT /v1/cart route is defined:
		else if ("/v1/cart".equals(path) && "PUT".equals(method)) {
			return handleProtectedEndpoint(request, claims ->
					cartController.submitOrder(request.getBody(), claims));
		}

		else if (path.matches("/v1/reservations/[^/]+") && "DELETE".equals(method)) {
			String reservationId = path.substring(path.lastIndexOf("/") + 1);
			return handleProtectedEndpoint(request, claims ->
					reservationDeletionController.cancelReservation(reservationId, claims));
		}

		// Endpoint not found
		return ApiResponse.notFound("Endpoint not found: " + path);
	}

	private APIGatewayProxyResponseEvent handleProtectedEndpoint(
			APIGatewayProxyRequestEvent request,
			java.util.function.Function<Map<String, Object>, APIGatewayProxyResponseEvent> handler) {

		// Validate token
		Map<String, Object> claims = authMiddleware.validateToken(request);
		if (claims == null) {
			return ApiResponse.unauthorized("Invalid or missing token");
		}

		// Call the protected endpoint handler
		APIGatewayProxyResponseEvent response = handler.apply(claims);

		// Check if we need to add new cookies from token refresh
		Map<String, Object> authorizer = request.getRequestContext().getAuthorizer();
		if (authorizer != null && authorizer.containsKey("responseHeaders")) {
			@SuppressWarnings("unchecked")
			Map<String, List<String>> responseHeaders =
					(Map<String, List<String>>) authorizer.get("responseHeaders");

			// Add the headers to the response
			response.setMultiValueHeaders(responseHeaders);
		}

		return response;
	}

	private APIGatewayProxyResponseEvent handleOptionsRequest() {
		return new APIGatewayProxyResponseEvent()
				.withStatusCode(200)
				.withHeaders(Map.of(
						"Access-Control-Allow-Origin", "*",
						"Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS",
						"Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
						"Access-Control-Allow-Credentials", "true"
				))
				.withBody("");
	}
}