package com.restaurantapp.Model;

import java.time.LocalDate;
import java.util.UUID;

public class CustomerFeedback {
    private String feedbackId;
    private String locationId;
    private String comment;
    private String rating;
    private String type;
    private LocalDate date;
    private String reservationId;
    private String customerEmail;
    private String waiterEmail;

    public CustomerFeedback() {
    }

    public CustomerFeedback(String feedbackId, String locationId, String comment, String rating,String type, LocalDate date, String reservationId, String customerEmail, String waiterEmail) {
        this.feedbackId = UUID.randomUUID().toString();
        this.locationId = locationId;
        this.comment = comment;
        this.rating = rating;
        this.type = type;
        this.date = date;
        this.reservationId = reservationId;
        this.customerEmail = customerEmail;
        this.waiterEmail = waiterEmail;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(String feedbackId) {
        this.feedbackId = feedbackId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }



    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getWaiterEmail() {
        return waiterEmail;
    }

    public void setWaiterEmail(String waiterEmail) {
        this.waiterEmail = waiterEmail;
    }
}
