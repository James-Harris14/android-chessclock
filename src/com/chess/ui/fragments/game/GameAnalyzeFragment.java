package com.chess.ui.fragments.game;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.chess.R;
import com.chess.backend.LoadHelper;
import com.chess.backend.LoadItem;
import com.chess.backend.RestHelper;
import com.chess.backend.ServerErrorCodes;
import com.chess.backend.entity.api.UserItem;
import com.chess.backend.tasks.RequestJsonTask;
import com.chess.model.GameAnalysisItem;
import com.chess.model.GameExplorerItem;
import com.chess.statics.AppConstants;
import com.chess.statics.StaticData;
import com.chess.statics.Symbol;
import com.chess.ui.engine.ChessBoard;
import com.chess.ui.engine.ChessBoardAnalysis;
import com.chess.ui.engine.FenHelper;
import com.chess.ui.engine.configs.CompGameConfig;
import com.chess.ui.fragments.comp.GameCompFragment;
import com.chess.ui.fragments.explorer.GameExplorerFragment;
import com.chess.ui.interfaces.boards.BoardFace;
import com.chess.ui.interfaces.game_ui.GameAnalysisFace;
import com.chess.ui.views.PanelInfoGameView;
import com.chess.ui.views.chess_boards.ChessBoardAnalysisView;
import com.chess.ui.views.chess_boards.NotationFace;
import com.chess.ui.views.drawables.BoardAvatarDrawable;
import com.chess.ui.views.drawables.IconDrawable;
import com.chess.ui.views.game_controls.ControlsAnalysisView;
import com.chess.utilities.AppUtils;
import com.chess.widgets.ProfileImageView;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 22.09.13
 * Time: 15:21
 */
public class GameAnalyzeFragment extends GameBaseFragment implements GameAnalysisFace {

	private static final String ERROR_TAG = "send request failed popup";

	protected static final String GAME_ITEM = "game_item";

	private ChessBoardAnalysisView boardView;
	protected GameAnalysisItem analysisItem;
	protected boolean userPlayWhite = true;
	protected ControlsAnalysisView controlsView;
	private String[] countryNames;
	private int[] countryCodes;
	private NotationFace notationsFace;

	public GameAnalyzeFragment() {

	}

	public static GameAnalyzeFragment createInstance(GameAnalysisItem analysisItem) {
		GameAnalyzeFragment fragment = new GameAnalyzeFragment();
		Bundle arguments = new Bundle();
		arguments.putParcelable(GAME_ITEM, analysisItem);
		fragment.setArguments(arguments);

		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments() != null) {
			analysisItem = getArguments().getParcelable(GAME_ITEM);
		} else {
			analysisItem = savedInstanceState.getParcelable(GAME_ITEM);
		}

		init();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.game_analysis_frame, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setTitle(R.string.analysis);

		widgetsInit(view);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (need2update) {
			adjustBoardForGame();
			need2update = false;
		} else {
			controlsView.enableGameControls(true);
			boardView.lockBoard(false);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		if (HONEYCOMB_PLUS_API) {
			dismissEndGameDialog();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable(GAME_ITEM, analysisItem);
	}

	@Override
	public void restart() {
		adjustBoardForGame();
	}

	@Override
	public void openNotes() {

		// don't used for non daily games
	}

	@Override
	public void closeBoard() {
		getActivityFace().showPreviousFragment();
	}

	@Override
	public void showExplorer() {
		GameExplorerItem explorerItem = new GameExplorerItem();
		explorerItem.setFen(getBoardFace().generateFullFen());
		explorerItem.setMovesList(getBoardFace().getMoveListSAN());
		explorerItem.setGameType(analysisItem.getGameType());
		getActivityFace().openFragment(GameExplorerFragment.createInstance(explorerItem));
	}

	@Override
	public void vsComputer() {
		int compGameMode = getAppData().getCompGameMode();
		if (compGameMode == AppConstants.GAME_MODE_COMPUTER_VS_COMPUTER) { // replace this fast speed fun
			compGameMode = AppConstants.GAME_MODE_COMPUTER_VS_PLAYER_WHITE;
			getAppData().setCompGameMode(compGameMode);
		}

		if (isUserColorWhite()) {
			compGameMode = AppConstants.GAME_MODE_COMPUTER_VS_PLAYER_WHITE;
		} else {
			compGameMode = AppConstants.GAME_MODE_COMPUTER_VS_PLAYER_BLACK;
		}

		CompGameConfig.Builder builder = new CompGameConfig.Builder()
				.setMode(compGameMode)
				.setStrength(getAppData().getCompLevel())
				.setFen(getBoardFace().generateFullFen());

		getActivityFace().openFragment(GameCompFragment.createInstance(builder.build()));
	}

	private void adjustBoardForGame() {
		resetBoardInstance();
		userPlayWhite = analysisItem.getUserSide() == ChessBoard.WHITE_SIDE;
		if (userPlayWhite) {
			labelsConfig.userSide = ChessBoard.WHITE_SIDE;
		} else {
			labelsConfig.userSide = ChessBoard.BLACK_SIDE;
		}

		analysisItem.fillLabelsConfig(labelsConfig);

		controlsView.enableGameControls(true);
		boardView.lockBoard(false);

		BoardFace boardFace = getBoardFace();
		boardFace.setFinished(false);

		topPanelView.showTimeLeftIcon(false);
		bottomPanelView.showTimeLeftIcon(false);

		if (analysisItem.getGameType() == RestHelper.V_GAME_CHESS_960) {
			boardFace.setChess960(true);
		} else {
			boardFace.setChess960(false);
		}

		boardFace.setupBoard(analysisItem.getFen()); // we better don't parse move than load incorrect fen

		boardFace.setReside(!userPlayWhite);

		String movesList = analysisItem.getMovesList();
		boolean allMovesWereMade = boardFace.checkAndParseMovesList(movesList);
		// if we open analysis from tactics we might have no movesList
		if (!allMovesWereMade && !TextUtils.isEmpty(movesList)) { // in case when we pass finished game from Comp we can't do anything here
			// do full reset of board
			resetBoardInstance();
			boardFace = getBoardFace();
			boardFace.setFinished(false);

			if (analysisItem.getGameType() == RestHelper.V_GAME_CHESS_960) {
				boardFace.setChess960(true);
			} else {
				boardFace.setChess960(false);
			}

			boardFace.setupBoard(FenHelper.DEFAULT_FEN);
			boardFace.setReside(!userPlayWhite);
			boardFace.checkAndParseMovesList(movesList);
		}

		boardView.resetValidMoves();

		invalidateGameScreen();
		boardFace.takeBack();

		playLastMoveAnimation();

		boardFace.setAnalysis(true);

		{// set avatars
			topAvatarImg = (ProfileImageView) topPanelView.findViewById(PanelInfoGameView.AVATAR_ID);
			bottomAvatarImg = (ProfileImageView) bottomPanelView.findViewById(PanelInfoGameView.AVATAR_ID);

			{ // set stubs while avatars are loading
				Drawable src = new IconDrawable(getActivity(), R.string.ic_profile,
						R.color.new_normal_grey_2, R.dimen.board_avatar_icon_size);

				labelsConfig.topAvatar = new BoardAvatarDrawable(getActivity(), src);

				labelsConfig.topAvatar.setSide(labelsConfig.getOpponentSide());
				topAvatarImg.setImageDrawable(labelsConfig.topAvatar);
				topPanelView.invalidateMe();

				labelsConfig.bottomAvatar = new BoardAvatarDrawable(getActivity(), src);

				labelsConfig.bottomAvatar.setSide(labelsConfig.userSide);
				bottomAvatarImg.setImageDrawable(labelsConfig.bottomAvatar);
				bottomPanelView.invalidateMe();
			}

			// todo: why check !contains(StaticData.GIF)
			if (!TextUtils.isEmpty(labelsConfig.topPlayerAvatar) && !labelsConfig.topPlayerAvatar.contains(StaticData.GIF)) {
				imageDownloader.download(labelsConfig.topPlayerAvatar, new ImageUpdateListener(ImageUpdateListener.TOP_AVATAR), AVATAR_SIZE);
			}

			// todo: why check !contains(StaticData.GIF)
			if (!TextUtils.isEmpty(labelsConfig.bottomPlayerAvatar) && !labelsConfig.bottomPlayerAvatar.contains(StaticData.GIF)) {
				imageDownloader.download(labelsConfig.bottomPlayerAvatar, new ImageUpdateListener(ImageUpdateListener.BOTTOM_AVATAR), AVATAR_SIZE);
			}

			// get opponent info
			if (!TextUtils.isEmpty(labelsConfig.topPlayerName)) {
				LoadItem loadItem = LoadHelper.getUserInfo(getUserToken(), labelsConfig.topPlayerName);
				new RequestJsonTask<UserItem>(new GetUserUpdateListener(GetUserUpdateListener.TOP_PLAYER)).executeTask(loadItem);
			}

			// get users info
			if (!TextUtils.isEmpty(labelsConfig.bottomPlayerName)) {
				LoadItem loadItem = LoadHelper.getUserInfo(getUserToken(), labelsConfig.bottomPlayerName);
				new RequestJsonTask<UserItem>(new GetUserUpdateListener(GetUserUpdateListener.BOTTOM_PLAYER)).executeTask(loadItem);
			}
		}
	}

	@Override
	public void toggleSides() { // TODO
		if (labelsConfig.userSide == ChessBoard.WHITE_SIDE) {
			labelsConfig.userSide = ChessBoard.BLACK_SIDE;
		} else {
			labelsConfig.userSide = ChessBoard.WHITE_SIDE;
		}
		BoardAvatarDrawable tempDrawable = labelsConfig.topAvatar;
		labelsConfig.topAvatar = labelsConfig.bottomAvatar;
		labelsConfig.bottomAvatar = tempDrawable;

		String tempLabel = labelsConfig.topPlayerName;
		labelsConfig.topPlayerName = labelsConfig.bottomPlayerName;
		labelsConfig.bottomPlayerName = tempLabel;

		String tempScore = labelsConfig.topPlayerRating;
		labelsConfig.topPlayerRating = labelsConfig.bottomPlayerRating;
		labelsConfig.bottomPlayerRating = tempScore;

		String playerTime = labelsConfig.topPlayerTime;
		labelsConfig.topPlayerTime = labelsConfig.bottomPlayerTime;
		labelsConfig.bottomPlayerTime = playerTime;

		int playerPremiumStatus = labelsConfig.topPlayerPremiumStatus;
		labelsConfig.topPlayerPremiumStatus = labelsConfig.bottomPlayerPremiumStatus;
		labelsConfig.bottomPlayerPremiumStatus = playerPremiumStatus;
	}

	@Override
	public void invalidateGameScreen() {
		if (labelsConfig.bottomAvatar != null) {
			labelsConfig.bottomAvatar.setSide(labelsConfig.userSide);
			bottomAvatarImg.setImageDrawable(labelsConfig.bottomAvatar);
		}

		if (labelsConfig.topAvatar != null) {
			labelsConfig.topAvatar.setSide(labelsConfig.getOpponentSide());
			topAvatarImg.setImageDrawable(labelsConfig.topAvatar);
		}

		topPanelView.setSide(labelsConfig.getOpponentSide());
		bottomPanelView.setSide(labelsConfig.userSide);

		topPanelView.setPlayerName(labelsConfig.topPlayerName);
		topPanelView.setPlayerRating(labelsConfig.topPlayerRating);
		bottomPanelView.setPlayerName(labelsConfig.bottomPlayerName);
		bottomPanelView.setPlayerRating(labelsConfig.bottomPlayerRating);

		topPanelView.setPlayerFlag(labelsConfig.topPlayerCountry);
		bottomPanelView.setPlayerFlag(labelsConfig.bottomPlayerCountry);


		topPanelView.setPlayerPremiumIcon(labelsConfig.topPlayerPremiumStatus);
		bottomPanelView.setPlayerPremiumIcon(labelsConfig.bottomPlayerPremiumStatus);

		boardView.updateNotations(getBoardFace().getNotationsArray());
	}

	@Override
	public String getWhitePlayerName() {
		if (labelsConfig.userSide == ChessBoard.BLACK_SIDE) {
			return Symbol.EMPTY;
		} else {
			return getUsername();
		}
	}

	@Override
	public String getBlackPlayerName() {
		if (labelsConfig.userSide == ChessBoard.WHITE_SIDE) {
			return Symbol.EMPTY;
		} else {
			return getUsername();
		}
	}

	@Override
	public boolean currentGameExist() {
		return true;
	}

	@Override
	public BoardFace getBoardFace() {
		if (chessBoard == null) {
			chessBoard = new ChessBoardAnalysis(this);
		}
		return chessBoard;
	}

	@Override
	public void updateAfterMove() {
	}

	@Override
	public void newGame() {

	}

	@Override
	public void switch2Analysis() {
	}

	@Override
	public Long getGameId() {
		return gameId;
	}

	@Override
	public void showOptions() {
	}

	@Override
	public void onPositiveBtnClick(DialogFragment fragment) {
		String tag = fragment.getTag();
		if (tag == null) {
			super.onPositiveBtnClick(fragment);
			return;
		}

		if (tag.equals(ERROR_TAG)) {
			backToLoginFragment();
		}
		super.onPositiveBtnClick(fragment);
	}

	@Override
	protected void restoreGame() {
		boardView.setGameActivityFace(this);

		adjustBoardForGame();
//		getBoardFace().setJustInitialized(false);
	}

	private NotationFace getNotationsFace() {
		return notationsFace;
	}

	private void setNotationsFace(View notationsView) {
		this.notationsFace = (NotationFace) notationsView;
	}

	private void init() {
		labelsConfig = new LabelsConfig();
		countryNames = getResources().getStringArray(R.array.new_countries);
		countryCodes = getResources().getIntArray(R.array.new_country_ids);

	}

	protected void widgetsInit(View view) {
		controlsView = (ControlsAnalysisView) view.findViewById(R.id.controlsView);
		if (inPortrait()) {
			setNotationsFace(view.findViewById(R.id.notationsView));
		} else {
			setNotationsFace(view.findViewById(R.id.notationsViewTablet));
		}
		topPanelView = (PanelInfoGameView) view.findViewById(R.id.topPanelView);
		bottomPanelView = (PanelInfoGameView) view.findViewById(R.id.bottomPanelView);

		{// set avatars
			Drawable src = new IconDrawable(getActivity(), R.string.ic_profile,
					R.color.new_normal_grey_2, R.dimen.board_avatar_icon_size);

			topAvatarImg = (ProfileImageView) topPanelView.findViewById(PanelInfoGameView.AVATAR_ID);
			bottomAvatarImg = (ProfileImageView) bottomPanelView.findViewById(PanelInfoGameView.AVATAR_ID);

			labelsConfig.topAvatar = new BoardAvatarDrawable(getActivity(), src);
			labelsConfig.bottomAvatar = new BoardAvatarDrawable(getActivity(), src);
		}

		controlsView.enableGameControls(false);
		controlsView.showVsComp(analysisItem.isAllowUseComp());

		boardView = (ChessBoardAnalysisView) view.findViewById(R.id.boardview);
		boardView.setFocusable(true);
		boardView.setTopPanelView(topPanelView);
		boardView.setBottomPanelView(bottomPanelView);
		boardView.setControlsAnalysisView(controlsView);
		boardView.setNotationsFace(getNotationsFace());

		setBoardView(boardView);

		boardView.setGameActivityFace(this);
		boardView.lockBoard(true);
	}

	protected class GetUserUpdateListener extends ChessUpdateListener<UserItem> {

		static final int BOTTOM_PLAYER = 0;
		static final int TOP_PLAYER = 1;

		private int itemCode;

		public GetUserUpdateListener(int itemCode) {
			super(UserItem.class);
			this.itemCode = itemCode;
		}

		@Override
		public void updateData(UserItem returnedObj) {
			super.updateData(returnedObj);
			UserItem.Data userInfo = returnedObj.getData();
			if (itemCode == BOTTOM_PLAYER) {
				labelsConfig.bottomPlayerCountry = AppUtils.getCountryIdByName(countryNames, countryCodes, userInfo.getCountryId());

				bottomPanelView.setPlayerFlag(labelsConfig.bottomPlayerCountry);
				bottomPanelView.setPlayerPremiumIcon(userInfo.getPremiumStatus());
			} else if (itemCode == TOP_PLAYER) {
				labelsConfig.topPlayerCountry = AppUtils.getCountryIdByName(countryNames, countryCodes, userInfo.getCountryId());

				topPanelView.setPlayerFlag(labelsConfig.topPlayerCountry);
				topPanelView.setPlayerPremiumIcon(userInfo.getPremiumStatus());
			}
		}

		@Override
		public void errorHandle(Integer resultCode) {
			if (RestHelper.containsServerCode(resultCode)) {
				int serverCode = RestHelper.decodeServerCode(resultCode);
				if (serverCode == ServerErrorCodes.RESOURCE_NOT_FOUND) {
					return;
				}
			}
			super.errorHandle(resultCode);
		}
	}

}
