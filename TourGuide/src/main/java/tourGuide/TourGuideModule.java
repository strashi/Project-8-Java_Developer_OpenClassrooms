package tourGuide;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gpsUtil.GpsUtil;
import rewardCentral.RewardCentral;
import tourGuide.service.RewardsService;

@Configuration
public class TourGuideModule {
	public static final boolean testMode = true;
	public static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
	public static final int defaultProximityBuffer = 10;

	public static final int attractionProximityRange = 200;
	@Bean
	public GpsUtil getGpsUtil() {
		return new GpsUtil();
	}
	
	/*@Bean
	public RewardsService getRewardsService() {
		return new RewardsService(getGpsUtil(), getRewardCentral());
	}*/
	
	@Bean
	public RewardCentral getRewardCentral() {
		return new RewardCentral();
	}
	
}
