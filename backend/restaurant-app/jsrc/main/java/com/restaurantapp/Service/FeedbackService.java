package com.restaurantapp.Service;

import com.restaurantapp.Model.*;
import com.restaurantapp.Repository.FeedbackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FeedbackService {
    private static final Logger LOG = LoggerFactory.getLogger(FeedbackService.class);
    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public FeedbackPageResponse getFeedbackByLocationAndType(String locationId, String type, int page, int size, List<String> sort) {
        try {
            LOG.info("Getting feedback for location: {}, type: {}, page: {}, size: {}", locationId, type, page, size);

            // Get feedback from repository
            List<Feedback> feedbacks = feedbackRepository.getFeedbackByLocationAndType(locationId, type, page, size, sort);
            LOG.info("Found {} feedbacks", feedbacks.toString());
            // Count total elements
            long totalElements = feedbackRepository.countFeedbackByLocationAndType(locationId, type);
            LOG.info(totalElements+" Feedbacks Fetched");
            // Calculate total pages
            int totalPages = (int) Math.ceil((double) totalElements / size);

            // Convert to response objects
            List<FeedbackResponse> content = feedbacks.stream()
                    .map(this::mapToFeedbackResponse)
                    .collect(Collectors.toList());
            // Create page info
            FeedbackPageResponse pageResponse = new FeedbackPageResponse();
            pageResponse.setContent(content);
            pageResponse.setTotalElements(totalElements);
            pageResponse.setTotalPages(totalPages);
            pageResponse.setSize(size);
            pageResponse.setNumber(page);
            pageResponse.setNumberOfElements(content.size());
            pageResponse.setFirst(page == 0);
            pageResponse.setLast(page >= totalPages - 1);
            pageResponse.setEmpty(content.isEmpty());

            // Set sort info
            if (sort != null && !sort.isEmpty()) {
                List<Sort> sortList = createSortList(sort);
                pageResponse.setSort(sortList);
            } else {
                pageResponse.setSort(new ArrayList<>());
            }

            // Set pageable info
            Pageable pageable = new Pageable();
            pageable.setOffset(page * size);
            pageable.setPageNumber(page);
            pageable.setPageSize(size);
            pageable.setPaged(true);
            pageable.setUnpaged(false);
            pageable.setSort(pageResponse.getSort());
            pageResponse.setPageable(pageable);
            return pageResponse;

        } catch (Exception e) {
            LOG.error("Error getting feedback", e);
            throw new RuntimeException("Failed to get feedback", e);
        }
    }

    private FeedbackResponse mapToFeedbackResponse(Feedback feedback) {
        FeedbackResponse response = new FeedbackResponse();
        response.setId(feedback.getId());
        response.setRate(feedback.getRate());
        response.setComment(feedback.getComment());
        response.setUserName(feedback.getUserName());
        response.setUserAvatarUrl(feedback.getUserAvatarUrl());
        response.setDate(feedback.getDate());
        response.setType(feedback.getType());
        response.setLocationId(feedback.getLocationId());
        return response;
    }

    private List<Sort> createSortList(List<String> sortCriteria) {
        List<Sort> sortList = new ArrayList<>();

        for (String criterion : sortCriteria) {
            String[] parts = criterion.split(",");
            String property = parts[0];
            boolean ascending = parts.length == 1 || "asc".equalsIgnoreCase(parts[1]);

            Sort sort = new Sort();
            sort.setProperty(property);
            sort.setAscending(ascending);
            sort.setDirection(ascending ? "ASC" : "DESC");
            sort.setIgnoreCase(true);
            sort.setNullHandling("NATIVE");
            sortList.add(sort);
        }

        return sortList;
    }
}