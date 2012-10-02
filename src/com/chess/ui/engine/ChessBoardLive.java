package com.chess.ui.engine;

import com.chess.ui.interfaces.BoardToGameActivityFace;

/**
 * ChessBoardLive class
 *
 * @author alien_roger
 * @created at: 27.09.12 21:15
 */
public class ChessBoardLive extends ChessBoard {

	private static ChessBoardLive instance;

	private ChessBoardLive(BoardToGameActivityFace gameActivityFace) {
		super(gameActivityFace);
	}

	public static ChessBoardLive getInstance(BoardToGameActivityFace gameActivityFace) {
		final Long gameId = gameActivityFace.getGameId();
		if (instance == null || instance.gameId == null || !instance.gameId.equals(gameId)) {
			instance = new ChessBoardLive(gameActivityFace);
			instance.gameId = gameId;
			instance.justInitialized = true;
		} else {
			instance.justInitialized = false;
		}
		return instance;
	}

	public static void resetInstance(){
		instance = null;
	}
}
