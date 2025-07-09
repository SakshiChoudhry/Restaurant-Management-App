package com.restaurantapp.Service;

import com.restaurantapp.Exception.UnauthorizedException;
import com.restaurantapp.Model.AnonymousFeedbackAuthResponse;
import com.restaurantapp.Model.Booking;
import com.restaurantapp.Model.AnonymousFeedbackAuthRequest;
import com.restaurantapp.Repository.BookingRepository;
import com.restaurantapp.Repository.WaiterRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Date;
import java.util.UUID;

@Singleton
public class AnonymousFeedbackService {
    private static final Logger LOG = LoggerFactory.getLogger(FeedbackService.class);
    private final BookingRepository bookingRepository;
    private final WaiterRepository waiterRepository;
    private final String jwtSecret = System.getenv("jwt_secret");
    private final long TOKEN_VALIDITY = 10 * 60 * 60 * 1000; // 10 hours in milliseconds

    @Inject
    public AnonymousFeedbackService(BookingRepository bookingRepository, WaiterRepository waiterRepository) {
        this.bookingRepository = bookingRepository;
        this.waiterRepository = waiterRepository;
    }

    /**
     * Authenticate a feedback request using reservation ID and se3 cret code
     * @param reservationId The reservation ID
     * @param secretCode The secret code
     * @return FeedbackAuthResponse containing access token and reservation details
     * @throws UnauthorizedException if authentication fails
     */
    public AnonymousFeedbackAuthResponse authenticateFeedback(String reservationId, String secretCode) {
        LOG.info("Authenticating feedback for reservation: {}", reservationId);

        // Get the booking by ID
        Booking booking = bookingRepository.getBookingById(reservationId);
        if (booking == null) {
            LOG.warn("Booking not found for reservation ID: {}", reservationId);
            throw new UnauthorizedException("Invalid reservation ID");
        }

        // Verify the secret code
        if (booking.getSecretCode() == null || !booking.getSecretCode().equals(secretCode)) {
            LOG.warn("Invalid secret code for reservation ID: {}", reservationId);
            throw new UnauthorizedException("Invalid secret code");
        }

        // Generate a temporary JWT token
        String token = generateFeedbackToken(booking);

        // Get waiter details
        String waiterName = waiterRepository.getWaiterName(booking.getWaiterEmail());
        String waiterImageUrl = waiterRepository.getWaiterImageUrl(booking.getWaiterEmail());
        String serviceRating = waiterRepository.getWaiterRating(booking.getWaiterEmail());

        // Create response
        AnonymousFeedbackAuthResponse response = new AnonymousFeedbackAuthResponse();
        response.setAccessToken(token);
        response.setReservationId(reservationId);
        response.setServiceRating(serviceRating);
        response.setWaiterImageUrl(waiterImageUrl);
        response.setWaiterName(waiterName);

        LOG.info("Feedback authentication successful for reservation: {}", reservationId);
        return response;
    }

    /**
     * Generate a JWT token for feedback
     * @param booking The booking
     * @return JWT token
     */
    private String generateFeedbackToken(Booking booking) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TOKEN_VALIDITY);

        return Jwts.builder()
                .setSubject("anonymous")
                .claim("reservationId", booking.getReservationId())
                .claim("locationId", booking.getLocationId())
                .claim("waiterEmail", booking.getWaiterEmail())
                .claim("role", "FEEDBACK")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString())
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }
}