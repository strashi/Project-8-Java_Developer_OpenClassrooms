package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.dto.CurrentLocationDTO;
import tourGuide.dto.NearByAttractionDTO;
import tourGuide.helper.InternalTestHelper;
import tourGuide.tracker.Tracker;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tourGuide.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;

import static tourGuide.TourGuideModule.testMode;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	public final ExecutorService executorService;

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		Locale.setDefault(Locale.US); // needed for GpsUtil to function
		this.rewardsService = rewardsService;
		
		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		executorService = Executors.newFixedThreadPool(600);
		addShutDownHook();
	}
	
	public List<UserReward> getUserRewards(User user) {
		rewardsService.calculateRewards(user);
		return user.getUserRewards();
	}
	
	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ?
					user.getLastVisitedLocation() :
					trackUserLocation(user);
		return visitedLocation;
	}
	
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}
	
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}
	
	public void addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}
	
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(), 
				user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		List<Provider> response = new ArrayList<>();
		for (Provider provider : providers){
			Money price = Money.of(provider.price,"USD");
			Money maxPrice = user.getUserPreferences().getHighPricePoint();
			Money minPrice = user.getUserPreferences().getLowerPricePoint();
			if(price.isGreaterThanOrEqualTo(minPrice) && price.isLessThanOrEqualTo(maxPrice))
				response.add(provider);
		}
		user.setTripDeals(response);
		return response;
	}


	public VisitedLocation trackUserLocation(User user) {
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
		executorService.submit(()->{
			user.addToVisitedLocations(visitedLocation);
			rewardsService.calculateRewards(user);
		});
		return visitedLocation;
	}

	public List<NearByAttractionDTO> getNearByAttractions(String userName) {

		Set<NearByAttractionDTO> nearbyAttractions = new TreeSet<>();
		Location userLocation = getUserLocation(getUser(userName)).location;

		for(Attraction attraction : gpsUtil.getAttractions()) {
			Location attractionLocation = new Location(attraction.latitude,attraction.longitude);
			double distance = rewardsService.getDistance(userLocation,attractionLocation);
			int rewardPoints = rewardsService.getRewardPoints(attraction,this.getUser(userName));
			NearByAttractionDTO nearByAttractionDTO = new NearByAttractionDTO(attraction,userLocation,distance, rewardPoints);
			nearbyAttractions.add(nearByAttractionDTO);
		}

		List<NearByAttractionDTO> response = new ArrayList<>();
		for(int i =0; i<5;i++){
			NearByAttractionDTO result = nearbyAttractions.stream().findFirst().get();
			response.add(result);
			nearbyAttractions.remove(result);
		}
		
		return response;
	}
	
	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() { 
		      public void run() {
		        tracker.stopTracking();
		      } 
		    }); 
	}

	/*****************************************************************
	 * When shutting down, ensure that all calculations are complete
	 *****************************************************************/

	public void stopTrackingUsersAndCompleteTasks() {
		tracker.stopTracking();
		logger.debug("Tracker stopped. Completing tasks . . .");
		executorService.shutdown();
		int minutes = 0;
		while (true) {
			try {
				if (executorService.awaitTermination(1, TimeUnit.MINUTES)) break;
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			logger.debug("Completing tasks . . . (elapsed {} minutes)", ++minutes);
		}
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			user.setUserPreferences(new UserPreferences());
			generateUserLocationHistory(user);
			//generateUserAttractionLocationHistory(user);
			//rewardsService.calculateRewards(user);
			internalUserMap.put(userName, user);

		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}
	
	private double generateRandomLongitude() {
		double leftLimit = -180;
	    double rightLimit = 180;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
	    double rightLimit = 85.05112878;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
	    return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

	//#################################################################

	private Attraction generateRandomAttraction(){
		List<Attraction> attractionList = gpsUtil.getAttractions();
		int leftLimit = 0;
		int rightLimit = 25;
		int intAttraction =  (int)(new Random().nextDouble() * (25+1));
		return attractionList.get(intAttraction);
	}
	private void generateUserAttractionLocationHistory(User user) {
			Attraction attraction = generateRandomAttraction();
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location((attraction.latitude), (attraction.longitude)), getRandomTime()));


	}

	public List<CurrentLocationDTO> getCurrentLocations() {
		List<CurrentLocationDTO> currentLocationDTOsList = new ArrayList<>();
		List<User> usersList = getAllUsers();
		for(User user: usersList){
			CurrentLocationDTO currentLocationDTO = new CurrentLocationDTO(user.getUserId(),user.getLastVisitedLocation().location);
			currentLocationDTOsList.add(currentLocationDTO);
		}
		return currentLocationDTOsList;
	}

	public UserPreferences getUserPreferences(String userName) {
		User user = getUser(userName);
		UserPreferences userPreferences = user.getUserPreferences();

		return userPreferences;
	}

	public void setUserPreferences(User user, int adults, int children, int nightsStay, double minPrice, double maxPrice) {
		UserPreferences preferences = user.getUserPreferences();
		preferences.setNumberOfAdults(adults);
		preferences.setNumberOfChildren(children);
		preferences.setTripDuration(nightsStay);
		preferences.setLowerPricePoint(Money.of(minPrice, "USD"));
		preferences.setHighPricePoint(Money.of(maxPrice, "USD"));
		user.setUserPreferences(preferences);
	}
}
