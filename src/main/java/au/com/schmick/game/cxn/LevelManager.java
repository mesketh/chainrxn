package au.com.schmick.game.cxn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 * 
 * @author mesketh
 * 
 */
public class LevelManager {

	public static class Level {

		private int maxBalls;
		private int minBallsToPass;
		private int ballScore;

		Level(String maxBalls, String minBallsToPass, String ballScore) {
			this.maxBalls = Integer.valueOf(maxBalls);
			this.minBallsToPass = Integer.valueOf(minBallsToPass);
			this.ballScore = Integer.valueOf(ballScore);
		}

		public int getMaxBalls() {
			return maxBalls;
		}

		public int getMinBallsToPass() {
			return minBallsToPass;
		}

		public int getBallScore() {
			return ballScore;
		}

	}

	private static List<Level> levelList = new ArrayList<Level>();
	private static Logger logger = LoggerFactory.getLogger(LevelManager.class);

	private static int levelNo = 0;

	static {
		loadLevels();
	}

	private static void loadLevels() {
		Properties props = new Properties();
		try {
			props.load(LevelManager.class.getResourceAsStream("/level/cxn.dat"));
			createLevels(props);
		} catch (IOException e) {
			logger.error("Failed to read cxn.dat");
		}
	}

	private static void createLevels(Properties props) {
		int numLevels = Integer.valueOf(props.getProperty("levels.max", "10"));
		if (numLevels > 0) {
			for (int i = 0; i < numLevels; i++) {
				String levelStr = String.format("level%s", i + 1);
				levelList.add(new Level(props.getProperty(levelStr
						+ ".max-balls"), props.getProperty(levelStr
						+ ".balls-to-pass"), props.getProperty(levelStr
						+ ".ball-score")));
			}
		} else {
			throw new IllegalArgumentException(
					"Check cxn.dat - no levels specified");
		}
	}

	public static Level getCurrentLevel() {
		return levelList.get(levelNo - 1);
	}

	public static void nextLevel() {
		levelNo++;
	}

	public static void resetLevels() {
		levelNo = 0;
	}

	public static boolean isLevelPassed(GameState gameState) {

		if ((LevelManager.getCurrentLevel().getMaxBalls() - gameState
				.getBalls().size()) >= getCurrentLevel().getMinBallsToPass()) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isLastLevel() {
		return levelNo == levelList.size();
	}

}
