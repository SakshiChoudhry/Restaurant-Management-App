package com.restaurantapp.Service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Controller.ApiResponse;
import com.restaurantapp.Exception.*;
import com.restaurantapp.Model.CustomerFeedback;
import com.restaurantapp.Model.CustomerFeedbackRequest;
import com.restaurantapp.Model.CustomerFeedbackResponse;
import com.restaurantapp.Model.Reservation;
import com.restaurantapp.Repository.CustomerFeedbackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Singleton
public class CustomerFeedbackService {

    private final ObjectMapper objectMapper;
    private CustomerFeedbackRepository customerFeedbackRepository;


    private static Logger LOG = LoggerFactory.getLogger(CustomerFeedbackService.class);

    @Inject
    public CustomerFeedbackService(CustomerFeedbackRepository customerFeedbackRepository) {
        this.customerFeedbackRepository = customerFeedbackRepository;
        this.objectMapper = new ObjectMapper();
    }

    public String createFeedback(CustomerFeedbackRequest request, String customerEmail) {

        String serviceFeedback = null;
        String cuisineFeedback = null;


        try {

            validation(request, customerEmail);

            Reservation customerFromDB = customerFeedbackRepository.checkForReservation(request, customerEmail, "create");

            if (customerFromDB == null) {
                throw new NotFoundException("This Reservation does not exist :(");
            }

            if (!customerFromDB.getCustomerEmail().equals(customerEmail)) {
                throw new UnauthorizedException("You are Not Allowed to Add Feedback TO this Reservation :(");
            }


            if ((request.getServiceRating() != null && !request.getServiceRating().trim().isEmpty()) &&
                    (request.getServiceComment() != null && !request.getServiceComment().trim().isEmpty())) {
                CustomerFeedback serviceCustomerFeedback = new CustomerFeedback(
                        "1",
                        customerFromDB.getLocationId(),
                        request.getServiceComment(),
                        request.getServiceRating(),
                        "SERVICE_EXPERIENCE",
                        LocalDate.now(),
                        customerFromDB.getReservationId(),
                        customerFromDB.getCustomerEmail(),
                        customerFromDB.getWaiterEmail()
                );
                serviceFeedback = customerFeedbackRepository.createFeedback(serviceCustomerFeedback);
            }

            if ((request.getCuisineRating() != null && !request.getCuisineRating().trim().isEmpty()) &&
                    (request.getCuisineComment() != null && !request.getCuisineComment().trim().isEmpty())) {
                CustomerFeedback serviceCustomerFeedback = new CustomerFeedback(
                        "1",
                        customerFromDB.getLocationId(),
                        request.getCuisineComment(),
                        request.getCuisineRating(),
                        "CUISINE_EXPERIENCE",
                        LocalDate.now(),
                        customerFromDB.getReservationId(),
                        customerFromDB.getCustomerEmail(),
                        customerFromDB.getWaiterEmail()
                );
                cuisineFeedback = customerFeedbackRepository.createFeedback(serviceCustomerFeedback);
            }

            if (serviceFeedback == null && cuisineFeedback == null) {
                throw new BadRequestException("No Feedback found :(");
            }

            // Handle the case where one or both feedbacks might be null
            if (serviceFeedback != null && serviceFeedback.equals("Feedback already exists") &&
                    cuisineFeedback != null && cuisineFeedback.equals("Feedback already exists")) {
                throw new ConflictException("Feedback already given!");
            }

            //Handle if only one value is passed
            if (serviceFeedback == null && cuisineFeedback != null && cuisineFeedback.equals("Feedback already exists")) {
                throw new ConflictException("Feedback already given!");
            }

            if (cuisineFeedback == null && serviceFeedback != null && serviceFeedback.equals("Feedback already exists")) {
                throw new ConflictException("Feedback already given!");
            }

            if (serviceFeedback != null && serviceFeedback.equals("Feedback already exists") &&
                    cuisineFeedback != null && !cuisineFeedback.equals("Feedback already exists")) {
                return "Cuisine Feedback added Successfully!";
            }

            if (serviceFeedback != null && !serviceFeedback.equals("Feedback already exists") &&
                    cuisineFeedback != null && cuisineFeedback.equals("Feedback already exists")) {
                return "Service Feedback added Successfully!";
            }


            return "Feedback Added Successfully";


        } catch (ForbiddenException e) {
            LOG.warn("Invalid booking request: {}", e.getMessage());
            throw e;
        }catch (NotFoundException e) {
            LOG.warn("Invalid booking request: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid booking request: {}", e.getMessage());
            throw e;
        } catch (BadRequestException e) {
            LOG.warn("Invalid booking request: {}", e.getMessage());
            throw e;
        } catch (PayloadTooLarge e) {
            LOG.warn("Invalid booking request: {}", e.getMessage());
            throw e;
        } catch (ConflictException e) {
            // Let this bubble up untouched!
            LOG.warn("Conflict during booking: {}", e.getMessage());
            throw e;
        } catch (UnprocessableException e) {
            // Let this bubble up untouched!
            LOG.warn("Processing Error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.error("Error creating booking", e);
            throw new RuntimeException("Error creating feedback: " + e.getMessage(), e);
        }

    }


    //UPDATE

    /**
     * Update feedback
     *
     * @param request       The feedback update request
     * @param customerEmail The customer email The feedback ID
     * @return A message indicating the result of the update
     */
    public String updateFeedback(CustomerFeedbackRequest request, String customerEmail) {
        String serviceFeedback = null;
        String cuisineFeedback = null;

        try {

            LOG.info("Updating feedback for customer: {}", customerEmail);

            // Validate the request
            validation(request, customerEmail);

            // Check if the user is authorized to update the feedback
            Reservation customerFromDB = customerFeedbackRepository.checkForReservation(request, customerEmail, "update");

            if (customerFromDB == null) {
                throw new NotFoundException("This Reservation does not exist :(");
            }

            if (!customerFromDB.getCustomerEmail().equals(customerEmail)) {
                throw new UnauthorizedException("You are Not Allowed to Add Feedback TO this Reservation :(");
            }


            if ((request.getServiceRating() != null && !request.getServiceRating().trim().isEmpty()) &&
                    (request.getServiceComment() != null && !request.getServiceComment().trim().isEmpty())) {
                CustomerFeedback serviceCustomerFeedback = new CustomerFeedback(
                        "1",
                        customerFromDB.getLocationId(),
                        request.getServiceComment(),
                        request.getServiceRating(),
                        "SERVICE_EXPERIENCE",
                        LocalDate.now(),
                        customerFromDB.getReservationId(),
                        customerFromDB.getCustomerEmail(),
                        customerFromDB.getWaiterEmail()
                );
                serviceFeedback = customerFeedbackRepository.updateTheFeedback(serviceCustomerFeedback);
            }

            if ((request.getCuisineRating() != null && !request.getCuisineRating().trim().isEmpty()) &&
                    (request.getCuisineComment() != null && !request.getCuisineComment().trim().isEmpty())) {
                CustomerFeedback serviceCustomerFeedback = new CustomerFeedback(
                        "1",
                        customerFromDB.getLocationId(),
                        request.getCuisineComment(),
                        request.getCuisineRating(),
                        "CUISINE_EXPERIENCE",
                        LocalDate.now(),
                        customerFromDB.getReservationId(),
                        customerFromDB.getCustomerEmail(),
                        customerFromDB.getWaiterEmail()
                );
                cuisineFeedback = customerFeedbackRepository.updateTheFeedback(serviceCustomerFeedback);
            }

            if (serviceFeedback == null && cuisineFeedback == null) {
                throw new BadRequestException("No Feedback Update found :(");
            }

            return "Feedback updated successfully";
        }catch (ForbiddenException e) {
            LOG.warn("Invalid booking request: {}", e.getMessage());
            throw e;
        } catch (NotFoundException e) {
            LOG.warn("Invalid booking request: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid booking request: {}", e.getMessage());
            throw e;
        } catch (BadRequestException e) {
            LOG.warn("Invalid booking request: {}", e.getMessage());
            throw e;
        } catch (PayloadTooLarge e) {
            LOG.warn("Invalid booking request: {}", e.getMessage());
            throw e;
        } catch (ConflictException e) {
            // Let this bubble up untouched!
            LOG.warn("Conflict during booking: {}", e.getMessage());
            throw e;
        } catch (UnprocessableException e) {
            // Let this bubble up untouched!
            LOG.warn("Processing Error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.error("Error creating booking", e);
            throw new RuntimeException("Error creating feedback: " + e.getMessage(), e);
        }
    }


    //VALIDATION
    public void validation(CustomerFeedbackRequest request, String customerEmail) {
        if (customerEmail == null) {
            throw new UnauthorizedException("User is Not Authorized :!");
        }
        if (request.getReservationId() == null || request.getReservationId().trim().isEmpty()) {
            throw new BadRequestException("Reservation cannot be null :!");
        }

        if ((request.getServiceRating() == null || request.getServiceRating().trim().isEmpty()) &&
                (request.getCuisineRating() == null || request.getCuisineRating().trim().isEmpty())) {
            throw new BadRequestException("Provide at least one customer rating :!");
        }


        if ((request.getCuisineComment() == null && request.getCuisineComment().trim().isEmpty()) ||
                (request.getServiceComment() == null && request.getServiceComment().trim().isEmpty())) {
            throw new BadRequestException(" Provide at least one customer comment :!");
        }

        // Check for Both
        if ((((request.getCuisineRating() != null) && (!request.getCuisineRating().trim().isEmpty())) &&
                ((request.getCuisineComment() == null) || (request.getCuisineComment().trim().isEmpty())))
                && (((request.getServiceRating() != null) && (!request.getServiceRating().trim().isEmpty())) &&
                ((request.getServiceComment() == null) || (request.getServiceComment().trim().isEmpty())))) {
            throw new BadRequestException("Provide Both Comments as Only rating is Not Allowed :(!");
        }


        if (((request.getCuisineRating() != null) && (!request.getCuisineRating().trim().isEmpty())) &&
                ((request.getCuisineComment() == null) || (request.getCuisineComment().trim().isEmpty()))) {
            throw new BadRequestException(" Provide Culinary Experience Comment: Only Rating Not Allowed!");
        }
        if (((request.getServiceRating() != null) && (!request.getServiceRating().trim().isEmpty())) &&
                ((request.getServiceComment() == null) || (request.getServiceComment().trim().isEmpty()))) {
            throw new BadRequestException(" Provide Service Experience Comment : Only Rating Not Allowed!");
        }

        // Limit checker for Ratings

        if (request.getServiceComment() != null && (!request.getServiceComment().trim().isEmpty()) && request.getCuisineRating() != null && (!request.getCuisineRating().trim().isEmpty())) {
            if ((Double.parseDouble(request.getServiceRating()) > 5 || Double.parseDouble(request.getServiceRating()) < 0) && (Double.parseDouble(request.getCuisineRating()) > 5 || Double.parseDouble(request.getCuisineRating()) < 0)) {
                throw new UnprocessableException("Service Rating and Cuisine Rating cannot be less than 0 or More than 5 :!");
            }
        }

        if (request.getServiceRating() != null && (!request.getServiceRating().trim().isEmpty())) {
            if ((Double.parseDouble(request.getServiceRating()) > 5) || (Double.parseDouble(request.getServiceRating()) < 0)) {
                throw new UnprocessableException("Service Rating cannot be less than 0 or More than 5 :!");
            }
        }
        if (request.getCuisineRating() != null && (!request.getCuisineRating().trim().isEmpty())) {
            if ((Double.parseDouble(request.getCuisineRating()) > 5) || (Double.parseDouble(request.getCuisineRating()) < 0)) {
                throw new UnprocessableException("Cuisine Rating cannot be less than 0 or More than 5 :!");
            }
        }
    }


    public List<CustomerFeedbackResponse> getAllFeedbacksForReservation(String reservationId, String customerEmail) {

        try {

            if (!customerFeedbackRepository.checkReservationForEmail(reservationId,customerEmail)){
                throw new UnauthorizedException("You cannot access this feedback for a reservation :(");
            }



            ScanResponse list = customerFeedbackRepository.getAllReservations(reservationId);

            List<CustomerFeedbackResponse> feedbacks = new ArrayList<>();


            for (Map<String, AttributeValue> i : list.items()) {

                CustomerFeedbackResponse tempResponse = new CustomerFeedbackResponse(
                        i.get("feedbackId").s(),
                        i.get("reservationId").s(),
                        i.get("rating").s(),
                        i.get("comment").s(),
                        i.get("customerEmail").s(),
                        i.get("waiterEmail").s(),
                        i.get("type").s()

                );
                feedbacks.add(tempResponse);

            }

            return feedbacks;

        }
        catch (BadRequestException e) {
            throw e;
        }catch (UnauthorizedException e) {
            throw e;
        }catch (Exception e) {
            throw e;
        }

    }
}
