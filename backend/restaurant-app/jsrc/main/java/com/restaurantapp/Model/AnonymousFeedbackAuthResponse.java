package com.restaurantapp.Model;

public class AnonymousFeedbackAuthResponse {
    private String accessToken;
    private String reservationId;
    private String serviceRating;
    private String waiterImageUrl;
    private String waiterName;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getServiceRating() {
        return serviceRating;
    }

    public void setServiceRating(String serviceRating) {
        this.serviceRating = serviceRating;
    }

    public String getWaiterImageUrl() {
        return waiterImageUrl;
    }

    public void setWaiterImageUrl(String waiterImageUrl) {
        this.waiterImageUrl = waiterImageUrl;
    }

    public String getWaiterName() {
        return waiterName;
    }

    public void setWaiterName(String waiterName) {
        this.waiterName = waiterName;
    }
}