package au.com.schmick.game.cxn;

import java.util.List;

public interface GameState {

	boolean isPaused();

	List<Reactor> getReactors();

	List<Ball> getBalls();
	
	int getCurrentScore();
	
	int getBallsLeft();
	
	boolean isAdvancing();

	boolean isGameFinished();
	
	boolean isGameStarted();
	
	void restartLevel();
	
	boolean isLevelOver();
}
