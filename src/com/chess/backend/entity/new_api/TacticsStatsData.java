package com.chess.backend.entity.new_api;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 04.02.13
 * Time: 15:24
 */
public class TacticsStatsData {
/*
    "tactics": {
      "current": 1311,
      "highest": {
        "rating": 1474,
        "timestamp": 1338361200
      },
      "lowest": {
        "rating": 0,
        "timestamp": 1338361200
      },
      "attempt_count": 53,
      "passed_count": 20,
      "failed_count": 33,
      "total_seconds": 3201
    }
*/
	private int current;
	private BaseRatingItem highest;
	private BaseRatingItem lowest;
	private int attempt_count;
	private int passed_count;
	private int failed_count;
	private long total_seconds;

	public int getCurrent() {
		return current;
	}

	public BaseRatingItem getHighest() {
		return highest;
	}

	public BaseRatingItem getLowest() {
		return lowest;
	}

	public int getAttemptCount() {
		return attempt_count;
	}

	public int getPassedCount() {
		return passed_count;
	}

	public int getFailedCount() {
		return failed_count;
	}

	public long getTotalSeconds() {
		return total_seconds;
	}
}
