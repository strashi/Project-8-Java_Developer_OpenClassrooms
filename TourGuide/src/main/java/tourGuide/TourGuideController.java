package tourGuide;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsoniter.output.JsonStream;

import gpsUtil.location.VisitedLocation;
import tourGuide.dto.CurrentLocationDTO;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tourGuide.user.UserReward;
import tripPricer.Provider;

@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public String getLocation(@RequestParam String userName) {
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
		return JsonStream.serialize(visitedLocation.location);
    }

    @RequestMapping("/getNearbyAttractions") 
    public String getNearbyAttractions(@RequestParam String userName) {
    	return JsonStream.serialize(tourGuideService.getNearByAttractions(userName));
    }
    
    @RequestMapping("/getRewards") 
    public String getRewards(@RequestParam String userName) {
    	List<UserReward> userRewards = tourGuideService.getUserRewards(getUser(userName));
        return JsonStream.serialize(userRewards);
    }
    
    @RequestMapping("/getAllCurrentLocations")
    public String getAllCurrentLocations() {
    	List<CurrentLocationDTO> currentLocations = tourGuideService.getCurrentLocations();
    	return JsonStream.serialize(currentLocations);
    }
    
    @RequestMapping("/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
    	List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
    	return JsonStream.serialize(providers);
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }

    //#################################################################

    @RequestMapping("/getUserPreferences")
    public UserPreferences getUserPreferences(@RequestParam String userName){
        UserPreferences userPreferences = tourGuideService.getUserPreferences(userName);
        return  userPreferences;

    }
    /* @RequestMapping("/getUserPreferences")
       public String getUserPreferences(@RequestParam String userName){
           UserPreferences userPreferences = tourGuideService.getUserPreferences(userName);
           return JsonStream.serialize("nombre adultes:" + userPreferences.getNumberOfAdults());

       }*/
    @RequestMapping(value = "/setUserPreferences")
    public String setUserPreferences(@RequestParam String userName,
                                     @RequestParam int adults,
                                     @RequestParam int children,
                                     @RequestParam int nights,
                                     @RequestParam double minPrice,
                                     @RequestParam double maxPrice) {
        tourGuideService.setUserPreferences(getUser(userName), adults, children, nights, minPrice, maxPrice);
        return JsonStream.serialize("Preferences updated" );
    }

}