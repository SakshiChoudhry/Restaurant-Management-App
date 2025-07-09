package com.restaurantapp.Model;

public class TimeSlot {
    private String SlotId;
    private String SlotStartTime;
    private String SlotEndTime;

    // Default constructor for Jackson
    public TimeSlot() {
    }

    public TimeSlot(String slotId, String slotStartTime, String slotEndTime) {
        SlotId = slotId;
        SlotStartTime = slotStartTime;
        SlotEndTime = slotEndTime;
    }

    public String getSlotId() {
        return SlotId;
    }

    public void setSlotId(String slotId) {
        SlotId = slotId;
    }

    public String getSlotStartTime() {
        return SlotStartTime;
    }

    public void setSlotStartTime(String slotStartTime) {
        SlotStartTime = slotStartTime;
    }

    public String getSlotEndTime() {
        return SlotEndTime;
    }

    public void setSlotEndTime(String slotEndTime) {
        SlotEndTime = slotEndTime;
    }
}