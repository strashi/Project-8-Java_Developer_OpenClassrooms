package tourGuide.dto;

import gpsUtil.location.Location;

import java.util.UUID;

public class CurrentLocationDTO {

    private final UUID userId;

    private Location currentLocation;

    public CurrentLocationDTO(UUID userId, Location currentLocation) {
        this.userId = userId;
        this.currentLocation = currentLocation;
    }
}
