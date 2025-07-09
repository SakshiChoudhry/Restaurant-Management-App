package com.restaurantapp.Model;

public class CustomerFeedbackResponse {
    private String feedbackId;
    private String reservationId;
    private String rating;
    private String comment;
    private String customerEmail;
    private String waiterEmail;
    private String type;

    public CustomerFeedbackResponse(String feedbackId, String reservationId, String rating, String comment, String customerEmail, String waiterEmail,String type) {
        this.feedbackId = feedbackId;
        this.reservationId = reservationId;
        this.rating = rating;
        this.comment = comment;
        this.customerEmail = customerEmail;
        this.waiterEmail = waiterEmail;
        this.type = type;
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

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
