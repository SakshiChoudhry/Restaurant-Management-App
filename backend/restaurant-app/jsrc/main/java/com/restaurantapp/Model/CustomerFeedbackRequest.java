package com.restaurantapp.Model;

public class CustomerFeedbackRequest {
    private String reservationId;
    private String cuisineComment="";
    private String cuisineRating="";
    private String serviceComment="";
    private String serviceRating="";

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getCuisineComment() {
        return cuisineComment;
    }

    public void setCuisineComment(String cuisineComment) {
        this.cuisineComment = cuisineComment;
    }

    public String getCuisineRating() {
        return cuisineRating;
    }

    public void setCuisineRating(String cuisineRating) {
        this.cuisineRating = cuisineRating;
    }

    public String getServiceComment() {
        return serviceComment;
    }

    public void setServiceComment(String serviceComment) {
        this.serviceComment = serviceComment;
    }

    public String getServiceRating() {
        return serviceRating;
    }

    public void setServiceRating(String serviceRating) {
        this.serviceRating = serviceRating;
    }

    public CustomerFeedbackRequest() {
    }

    public CustomerFeedbackRequest(String reservationId, String cuisineComment, String cuisineRating, String serviceComment, String serviceRating) {
        this.reservationId = reservationId;
        this.cuisineComment = cuisineComment;
        this.cuisineRating = cuisineRating;
        this.serviceComment = serviceComment;
        this.serviceRating = serviceRating;
    }
}
