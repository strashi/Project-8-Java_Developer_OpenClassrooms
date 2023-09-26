package tourGuide.dto;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;

public class NearByAttractionDTO implements Comparable<NearByAttractionDTO>{

    //  TODO: Change this method to no longer return a List of Attractions.
    //  Instead: Get the closest five tourist attractions to the user - no matter how far away they are.
    //  Return a new JSON object that contains:
    // Name of Tourist attraction,
    // Tourist attractions lat/long,
    // The user's location lat/long,
    // The distance in miles between the user's location and each of the attractions.
    // The reward points for visiting each Attraction.
    //    Note: Attraction reward points can be gathered from RewardsCentral

    public String attractionName;
    public Location attractionLocation;
    public Location userLocation;
    public double distanceInMiles;
    public int rewardPoints;

    public NearByAttractionDTO(Attraction attraction, Location userLocation, double distanceInMiles, int rewardPoints) {
        this.attractionName = attraction.attractionName;
        this.attractionLocation = new Location(attraction.latitude, attraction.longitude);
        this.userLocation = userLocation;
        this.distanceInMiles = distanceInMiles;
        this.rewardPoints = rewardPoints;
    }
    public int compareTo(NearByAttractionDTO nearByAttractionDTO) {

        if (distanceInMiles > nearByAttractionDTO.distanceInMiles) {
            return 1;
        }
        else if (distanceInMiles < nearByAttractionDTO.distanceInMiles) {
            return -1;
        }
        else {
            return 0;
        }

    }



}
