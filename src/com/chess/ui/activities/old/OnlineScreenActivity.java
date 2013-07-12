package com.chess.ui.activities.old;

import android.app.AlertDialog;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.chess.R;
import com.chess.backend.RestHelper;
import com.chess.backend.entity.LoadItem;
import com.chess.backend.entity.new_api.*;
import com.chess.backend.interfaces.ActionBarUpdateListener;
import com.chess.backend.statics.AppConstants;
import com.chess.backend.statics.IntentConstants;
import com.chess.backend.statics.StaticData;
import com.chess.backend.tasks.RequestJsonTask;
import com.chess.db.DBDataManager;
import com.chess.db.DbHelper;
import com.chess.db.tasks.LoadDataFromDbTask;
import com.chess.db.tasks.SaveDailyCurrentGamesListTask;
import com.chess.db.tasks.SaveDailyFinishedGamesListTask;
import com.chess.model.BaseGameItem;
import com.chess.model.GameOnlineItem;
import com.chess.ui.activities.LiveBaseActivity;
import com.chess.ui.adapters.CustomSectionedAdapter;
import com.chess.ui.adapters.DailyChallengesGamesAdapter;
import com.chess.ui.adapters.DailyCurrentGamesCursorAdapter;
import com.chess.ui.adapters.DailyFinishedGamesCursorAdapter;
import com.chess.ui.engine.ChessBoardOnline;
import com.chess.ui.interfaces.ItemClickListenerFace;
import com.chess.utilities.AppUtils;

/**
 * OnlineScreenActivity class
 *
 * @author alien_roger
 * @created at: 08.02.12 7:12
 */
public class OnlineScreenActivity extends LiveBaseActivity implements View.OnClickListener,
		AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, ItemClickListenerFace {
	private static final int CURRENT_GAMES_SECTION = 0;
	private static final int CHALLENGES_SECTION = 1;

	private static final String DRAW_OFFER_PENDING_TAG = "DRAW_OFFER_PENDING_TAG";
	private static final String CHALLENGE_ACCEPT_TAG = "challenge accept popup";
	private static final String UNABLE_TO_MOVE_TAG = "unable to move popup";

	private int successToastMsgId;

	private OnlineUpdateListener challengeInviteUpdateListener;
	private OnlineUpdateListener acceptDrawUpdateListener;
	private LoadItem selectedLoadItem;
	private DailyCurrentGamesCursorAdapter currentGamesCursorAdapter;
	private DailyChallengesGamesAdapter challengesGamesAdapter;
	private DailyFinishedGamesCursorAdapter finishedGamesCursorAdapter;
	private CustomSectionedAdapter sectionedAdapter;
	private DailyCurrentGameData gameListCurrentItem;
	private DailyChallengeItem.Data gameListChallengeItem;
	private boolean onVacation;
	private IntentFilter listUpdateFilter;
	private BroadcastReceiver gamesUpdateReceiver;
	private SaveCurrentGamesListUpdateListener saveCurrentGamesListUpdateListener;
	private SaveFinishedGamesListUpdateListener saveFinishedGamesListUpdateListener;
	private GamesCursorUpdateListener currentGamesCursorUpdateListener;
	private GamesCursorUpdateListener finishedGamesCursorUpdateListener;
	private DailyGamesUpdateListener dailyGamesUpdateListener;
	private VacationUpdateListener vacationDeleteUpdateListener;
	private VacationUpdateListener vacationGetUpdateListener;
	private TextView emptyView;
	private ListView listView;
	private View loadingView;
	private boolean hostUnreachable;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.online_screen);
		Log.d("GetStringObjTask", " ONLINE OnCreate");

		getAppData().setLiveChessMode(false);
		// init adapters
		sectionedAdapter = new CustomSectionedAdapter(this, R.layout.new_daily_challenge_game_item);

		challengesGamesAdapter = new DailyChallengesGamesAdapter(this, null);
		currentGamesCursorAdapter = new DailyCurrentGamesCursorAdapter(getContext(), null);
		finishedGamesCursorAdapter = new DailyFinishedGamesCursorAdapter(getContext(), null);

		sectionedAdapter.addSection(getString(R.string.current_games), currentGamesCursorAdapter);
		sectionedAdapter.addSection(getString(R.string.challenges), challengesGamesAdapter);
		sectionedAdapter.addSection(getString(R.string.finished_games), finishedGamesCursorAdapter);

		loadingView = findViewById(R.id.loadingView);
		emptyView = (TextView) findViewById(R.id.emptyView);

		listView = (ListView) findViewById(R.id.listView);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		listView.setAdapter(sectionedAdapter);

		findViewById(R.id.tournaments).setOnClickListener(this);
		findViewById(R.id.statsBtn).setOnClickListener(this);

		listUpdateFilter = new IntentFilter(IntentConstants.USER_MOVE_UPDATE);

		initUpgradeAndAdWidgets();
		/*moPubView = (MoPubView) findViewById(R.id.mopub_adview); // init anyway as it is declared in layout
        MopubHelper.showBannerAd(upgradeBtn, moPubView, this);*/
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d("GetStringObjTask", " ONLINE OnRestart");

	}

	@Override
	protected void onStart() {

		super.onStart();
		init();

		gamesUpdateReceiver = new GamesUpdateReceiver();
		registerReceiver(gamesUpdateReceiver, listUpdateFilter);

		if (AppUtils.isNetworkAvailable(this) && !isRestarted) {
			updateVacationStatus();
			updateGamesList();
		} else {
			emptyView.setText(R.string.no_network);
			showEmptyView(true);
		}

		if (DBDataManager.haveSavedDailyGame(this, getCurrentUserName())) {
			loadDbGames();
		}
	}

	private void init() {
		selectedLoadItem = new LoadItem();

		challengeInviteUpdateListener = new OnlineUpdateListener(OnlineUpdateListener.INVITE);
		acceptDrawUpdateListener = new OnlineUpdateListener(OnlineUpdateListener.DRAW);
		saveCurrentGamesListUpdateListener = new SaveCurrentGamesListUpdateListener();
		saveFinishedGamesListUpdateListener = new SaveFinishedGamesListUpdateListener();
		currentGamesCursorUpdateListener = new GamesCursorUpdateListener(GamesCursorUpdateListener.CURRENT);
		finishedGamesCursorUpdateListener = new GamesCursorUpdateListener(GamesCursorUpdateListener.FINISHED);

		dailyGamesUpdateListener = new DailyGamesUpdateListener();
		vacationGetUpdateListener = new VacationUpdateListener(VacationUpdateListener.GET);
		vacationDeleteUpdateListener = new VacationUpdateListener(VacationUpdateListener.DELETE);
	}

	@Override
	protected void onStop() {
		super.onStop();

		unRegisterMyReceiver(gamesUpdateReceiver);

		releaseResources();
	}

	@Override
	public Context getMeContext() {
		return this;
	}

	private class SaveCurrentGamesListUpdateListener extends ActionBarUpdateListener<DailyCurrentGameData> {
		public SaveCurrentGamesListUpdateListener() {
			super(getInstance());
		}

		@Override
		public void showProgress(boolean show) {
			super.showProgress(show);
			showLoadingView(show);
		}

		@Override
		public void updateData(DailyCurrentGameData returnedObj) {
			new LoadDataFromDbTask(currentGamesCursorUpdateListener, DbHelper.getDailyCurrentMyListGamesParams(getCurrentUserName()),
					getContentResolver()).executeTask();
		}
	}

	private class SaveFinishedGamesListUpdateListener extends ActionBarUpdateListener<DailyFinishedGameData> {
		public SaveFinishedGamesListUpdateListener() {
			super(getInstance());
		}

		@Override
		public void showProgress(boolean show) {
			super.showProgress(show);
			showLoadingView(show);
		}

		@Override
		public void updateData(DailyFinishedGameData returnedObj) {
			new LoadDataFromDbTask(finishedGamesCursorUpdateListener, DbHelper.getDailyFinishedListGamesParams(getCurrentUserName()),
					getContentResolver()).executeTask();
		}
	}

	private class GamesCursorUpdateListener extends ActionBarUpdateListener<Cursor> {
		public static final int CURRENT = 0;
		public static final int FINISHED = 1;

		private int gameType;

		public GamesCursorUpdateListener(int gameType) {
			super(getInstance());
			this.gameType = gameType;
		}

		@Override
		public void showProgress(boolean show) {
			super.showProgress(show);
			showLoadingView(show);
		}

		@Override
		public void updateData(Cursor returnedObj) {
			super.updateData(returnedObj);

			switch (gameType) {
				case CURRENT:
					currentGamesCursorAdapter.changeCursor(returnedObj);
					if (AppUtils.isNetworkAvailable(getContext()) && !hostUnreachable && !isRestarted) {
//						updateGamesList();
					} else {
						new LoadDataFromDbTask(finishedGamesCursorUpdateListener,
								DbHelper.getDailyFinishedListGamesParams(getCurrentUserName()),
								getContentResolver()).executeTask();
					}

					break;
				case FINISHED:
					finishedGamesCursorAdapter.changeCursor(returnedObj);
					break;
			}
		}

		@Override
		public void errorHandle(Integer resultCode) {
			super.errorHandle(resultCode);
			if (resultCode == StaticData.EMPTY_DATA) {
				emptyView.setText(R.string.no_games);
			} else if (resultCode == StaticData.UNKNOWN_ERROR) {
				emptyView.setText(R.string.no_network);
			}
			showEmptyView(true);
		}
	}

	private class DailyGamesUpdateListener extends ActionBarUpdateListener<DailyGamesAllItem> {

		public DailyGamesUpdateListener() {
			super(getInstance(), DailyGamesAllItem.class);
		}

		@Override
		public void updateData(DailyGamesAllItem returnedObj) {
			hostUnreachable = false;
			challengesGamesAdapter.setItemsList(returnedObj.getData().getChallenges());
			new SaveDailyCurrentGamesListTask(saveCurrentGamesListUpdateListener, returnedObj.getData().getCurrent(),
					getContentResolver()).executeTask();
			new SaveDailyFinishedGamesListTask(saveFinishedGamesListUpdateListener, returnedObj.getData().getFinished(),
					getContentResolver()).executeTask();
		}

		@Override
		public void errorHandle(Integer resultCode) {
			if (resultCode == StaticData.INTERNAL_ERROR) {
				emptyView.setText("Internal error occurred");
				showEmptyView(true);
			}
		}
	}

	private class OnlineUpdateListener extends ActionBarUpdateListener<BaseResponseItem> {
		public static final int INVITE = 3;
		public static final int DRAW = 4;
		public static final int VACATION = 5;

		private int itemCode;

		public OnlineUpdateListener(int itemCode) {
			super(getInstance(),BaseResponseItem.class);
			this.itemCode = itemCode;
		}

		@Override
		public void showProgress(boolean show) {
			super.showProgress(show);
			showLoadingView(show);
		}

		@Override
		public void updateData(BaseResponseItem returnedObj) {
			if (isPaused || isFinishing()) {
				return;
			}

			switch (itemCode) {
				case INVITE:
					showToast(successToastMsgId);
					updateGamesList();
					break;
				case DRAW:
					updateGamesList();
					break;
				case VACATION:

					break;
			}
		}

		@Override
		public void errorHandle(String resultMessage) {
			// redundant check? we already clean the tasks pool in onPause, or...?
			// No, cleaning the task pool doesn't stop task immediately if it already reached onPOstExecute state.
			// this check prevent illegalStateExc for fragments, when they showed after onSavedInstance was called
			if (isPaused)
				return;

			if (resultMessage.equals(RestHelper.R_YOU_ARE_ON_VACATION)) {
				showToast(R.string.no_challenges_during_vacation);
			} else {
				showSinglePopupDialog(R.string.error, resultMessage);
			}
		}

		@Override
		public void errorHandle(Integer resultCode) {
			if (itemCode == GameOnlineItem.CURRENT_TYPE || itemCode == GameOnlineItem.CHALLENGES_TYPE
					|| itemCode == GameOnlineItem.FINISHED_TYPE) {
				if (resultCode == StaticData.NO_NETWORK || resultCode == StaticData.UNKNOWN_ERROR) {
					showToast(R.string.host_unreachable_load_local);
					hostUnreachable = true;
					loadDbGames();
				}
			}
		}
	}

	private void updateVacationStatus() {
		LoadItem loadItem = new LoadItem();
		loadItem.setLoadPath(RestHelper.CMD_VACATIONS);
		loadItem.setRequestMethod(RestHelper.GET);
		loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, getAppData().getUserToken());

//		new GetStringObjTask(vacationStatusUpdateListener).execute(loadItem);
		new RequestJsonTask<VacationItem>(vacationGetUpdateListener).executeTask(loadItem);
	}

	@Override
	public void onPositiveBtnClick(DialogFragment fragment) {
		String tag = fragment.getTag();
		if (tag == null) {
			super.onPositiveBtnClick(fragment);
			return;
		}

		if (tag.equals(DRAW_OFFER_PENDING_TAG)) {
			LoadItem loadItem = new LoadItem();
			loadItem.setLoadPath(RestHelper.CMD_PUT_GAME_ACTION(gameListCurrentItem.getGameId()));
			loadItem.setRequestMethod(RestHelper.PUT);
			loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, getAppData().getUserToken());
			loadItem.addRequestParams(RestHelper.P_COMMAND, RestHelper.V_ACCEPTDRAW);
			loadItem.addRequestParams(RestHelper.P_TIMESTAMP, gameListCurrentItem.getTimestamp());

//			new GetStringObjTask(acceptDrawUpdateListener).executeTask(loadItem);
			new RequestJsonTask<BaseResponseItem>(acceptDrawUpdateListener).executeTask(loadItem);
			new RequestJsonTask<BaseResponseItem>(acceptDrawUpdateListener).executeTask(loadItem);
		} else if (tag.equals(CHALLENGE_ACCEPT_TAG)) {
			LoadItem loadItem = new LoadItem();
			loadItem.setLoadPath(RestHelper.CMD_ANSWER_GAME_SEEK(gameListChallengeItem.getGameId()));
			loadItem.setRequestMethod(RestHelper.PUT);
			loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, getAppData().getUserToken());
			successToastMsgId = R.string.challenge_accepted;

//			new GetStringObjTask(challengeInviteUpdateListener).executeTask(loadItem);
			new RequestJsonTask<BaseResponseItem>(challengeInviteUpdateListener).executeTask(loadItem);
		}
		super.onPositiveBtnClick(fragment);
	}

	@Override
	public void onNeutralBtnCLick(DialogFragment fragment) {
		String tag = fragment.getTag();
		if (tag == null) {
			super.onNeutralBtnCLick(fragment);
			return;
		}

		if (tag.equals(DRAW_OFFER_PENDING_TAG)) {
			LoadItem loadItem = new LoadItem();
			loadItem.setLoadPath(RestHelper.CMD_PUT_GAME_ACTION(gameListCurrentItem.getGameId()));
			loadItem.setRequestMethod(RestHelper.PUT);
			loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, getAppData().getUserToken());
			loadItem.addRequestParams(RestHelper.P_COMMAND, RestHelper.V_DECLINEDRAW);
			loadItem.addRequestParams(RestHelper.P_TIMESTAMP, gameListCurrentItem.getTimestamp());

//			new GetStringObjTask(acceptDrawUpdateListener).executeTask(loadItem);
			new RequestJsonTask<BaseResponseItem>(acceptDrawUpdateListener).executeTask(loadItem);
		}
		super.onNeutralBtnCLick(fragment);
	}

	@Override
	public void onNegativeBtnClick(DialogFragment fragment) {
		String tag = fragment.getTag();
		if (tag == null) {
			super.onNegativeBtnClick(fragment);
			return;
		}

		if (tag.equals(DRAW_OFFER_PENDING_TAG)) {
			ChessBoardOnline.resetInstance();

//			Intent intent = new Intent(getContext(), GameOnlineScreenActivity.class);
//			intent.putExtra(BaseGameItem.GAME_ID, gameListCurrentItem.getGameId());
//			startActivity(intent);

		} else if (tag.equals(CHALLENGE_ACCEPT_TAG)) {
			LoadItem loadItem = new LoadItem();
			loadItem.setLoadPath(RestHelper.CMD_ANSWER_GAME_SEEK(gameListChallengeItem.getGameId()));
			loadItem.setRequestMethod(RestHelper.DELETE);
			loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, getAppData().getUserToken());
			successToastMsgId = R.string.challenge_declined;

//			new GetStringObjTask(challengeInviteUpdateListener).executeTask(loadItem);
			new RequestJsonTask<BaseResponseItem>(challengeInviteUpdateListener).executeTask(loadItem);
		} else if (tag.equals(UNABLE_TO_MOVE_TAG)) {
			LoadItem loadItem = new LoadItem();
			loadItem.setLoadPath(RestHelper.CMD_VACATIONS);
			loadItem.setRequestMethod(RestHelper.DELETE);
			loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, getAppData().getUserToken());

			new RequestJsonTask<VacationItem>(vacationDeleteUpdateListener).executeTask(loadItem);
		}
		super.onNegativeBtnClick(fragment);
	}

	private class VacationUpdateListener extends ActionBarUpdateListener<VacationItem> {

		static final int GET = 0;
		static final int DELETE = 1;
		private int listenerCode;

		public VacationUpdateListener(int listenerCode) {
			super(getInstance(), VacationItem.class);
			this.listenerCode = listenerCode;
		}

		@Override
		public void updateData(VacationItem returnedObj) {
			switch (listenerCode) {
				case GET:
					onVacation = returnedObj.getData().isOnVacation();
					break;
				case DELETE:
					onVacation = false;
					updateGamesList();
					break;
			}
		}
	}


	private DialogInterface.OnClickListener gameListItemDialogListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface d, int pos) {
			if (pos == 0) {
//				preferencesEditor.putString(AppConstants.OPPONENT, gameListCurrentItem.getOpponentUsername());
//				preferencesEditor.commit();

				Intent intent = new Intent(getContext(), ChatOnlineActivity.class);
				intent.putExtra(BaseGameItem.GAME_ID, gameListCurrentItem.getGameId());
				startActivity(intent);
			} else if (pos == 1) {
				String draw = RestHelper.V_OFFERDRAW;
				if (gameListCurrentItem.isDrawOffered())
					draw = RestHelper.V_ACCEPTDRAW;

				LoadItem loadItem = new LoadItem();
				loadItem.setLoadPath(RestHelper.CMD_PUT_GAME_ACTION(gameListCurrentItem.getGameId()));
				loadItem.setRequestMethod(RestHelper.PUT);
				loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, getAppData().getUserToken());
				loadItem.addRequestParams(RestHelper.P_COMMAND, draw);
				loadItem.addRequestParams(RestHelper.P_TIMESTAMP, gameListCurrentItem.getTimestamp());

//				new GetStringObjTask(acceptDrawUpdateListener).executeTask(loadItem);
				new RequestJsonTask<BaseResponseItem>(acceptDrawUpdateListener).executeTask(loadItem);
			} else if (pos == 2) {

				LoadItem loadItem = new LoadItem();
				loadItem.setLoadPath(RestHelper.CMD_PUT_GAME_ACTION(gameListCurrentItem.getGameId()));
				loadItem.setRequestMethod(RestHelper.PUT);
				loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, getAppData().getUserToken());
				loadItem.addRequestParams(RestHelper.P_COMMAND, RestHelper.V_RESIGN);
				loadItem.addRequestParams(RestHelper.P_TIMESTAMP, gameListCurrentItem.getTimestamp());

//				new GetStringObjTask(acceptDrawUpdateListener).executeTask(loadItem);
				new RequestJsonTask<BaseResponseItem>(acceptDrawUpdateListener).executeTask(loadItem);
			}
		}
	};

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.upgradeBtn) {
			startActivity(getAppData().getMembershipAndroidIntent());
		} else if (view.getId() == R.id.tournaments) {

			String playerTournamentsLink = RestHelper.formTournamentsLink(getAppData().getUserToken());
			Intent intent = new Intent(this, WebViewActivity.class);
			intent.putExtra(AppConstants.EXTRA_WEB_URL, playerTournamentsLink);
			intent.putExtra(AppConstants.EXTRA_TITLE, getString(R.string.tournaments));
			startActivity(intent);

		} else if (view.getId() == R.id.statsBtn) {

			String playerStatsLink = RestHelper.formStatsLink(getAppData().getUserToken(), getAppData().getUserName());
			Intent intent = new Intent(this, WebViewActivity.class);
			intent.putExtra(AppConstants.EXTRA_WEB_URL, playerStatsLink);
			intent.putExtra(AppConstants.EXTRA_TITLE, getString(R.string.stats));
			startActivity(intent);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
		int section = sectionedAdapter.getCurrentSection(pos);

		if (section == CURRENT_GAMES_SECTION) {
			if (onVacation) {
				popupItem.setNegativeBtnId(R.string.end_vacation);
				showPopupDialog(R.string.unable_to_move_on_vacation, UNABLE_TO_MOVE_TAG);
				return;
			}

			Cursor cursor = (Cursor) adapterView.getItemAtPosition(pos);
			gameListCurrentItem = DBDataManager.getDailyCurrentGameFromCursor(cursor);

//			preferencesEditor.putString(AppConstants.OPPONENT, gameListCurrentItem.getOpponentUsername());
//			preferencesEditor.commit();

			if (gameListCurrentItem.isDrawOffered()) {
				popupItem.setPositiveBtnId(R.string.accept);
				popupItem.setNeutralBtnId(R.string.decline);
				popupItem.setNegativeBtnId(R.string.game);

				showPopupDialog(R.string.accept_draw_q, DRAW_OFFER_PENDING_TAG);
				getLastPopupFragment().setButtons(3);

			} else {
				ChessBoardOnline.resetInstance();
//				Intent intent = new Intent(getContext(), GameOnlineScreenActivity.class);
//				intent.putExtra(BaseGameItem.GAME_ID, gameListCurrentItem.getGameId());
//
//				startActivity(intent);
			}
		} else if (section == CHALLENGES_SECTION) {
			clickOnChallenge((DailyChallengeItem.Data) adapterView.getItemAtPosition(pos));
		} else {

//			Cursor cursor = (Cursor) adapterView.getItemAtPosition(pos);
//			GameListFinishedItem finishedItem = DBDataManager.getDailyFinishedListGameFromCursor(cursor);
//			preferencesEditor.putString(AppConstants.OPPONENT, finishedItem.getOpponentUsername());
//			preferencesEditor.commit();

//			Intent intent = new Intent(getContext(), GameFinishedScreenActivity.class);
//			intent.putExtra(BaseGameItem.GAME_ID, finishedItem.getGameId());
//			startActivity(intent);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long l) {
		int section = sectionedAdapter.getCurrentSection(pos);

		if (section == CURRENT_GAMES_SECTION) {
			Cursor cursor = (Cursor) adapterView.getItemAtPosition(pos);
			gameListCurrentItem = DBDataManager.getDailyCurrentGameFromCursor(cursor);

			new AlertDialog.Builder(getContext())
					.setItems(new String[]{
							getString(R.string.chat),
							getString(R.string.offer_draw),
							getString(R.string.resign_or_abort)},
							gameListItemDialogListener)
					.create().show();

		} else if (section == CHALLENGES_SECTION) {
			clickOnChallenge((DailyChallengeItem.Data) adapterView.getItemAtPosition(pos));
		} else {
//			Cursor cursor = (Cursor) adapterView.getItemAtPosition(pos);
//			GameListFinishedItem finishedItem = DBDataManager.getDailyFinishedListGameFromCursor(cursor);
//
//			preferencesEditor.putString(AppConstants.OPPONENT, finishedItem.getOpponentUsername());
//			preferencesEditor.commit();
//
//			Intent intent = new Intent(getContext(), ChatOnlineActivity.class);
//			intent.putExtra(BaseGameItem.GAME_ID, finishedItem.getGameId());
//			startActivity(intent);
		}
		return true;
	}

	private class GamesUpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateGamesList();
		}
	}

	private void clickOnChallenge(DailyChallengeItem.Data gameListChallengeItem) {
		this.gameListChallengeItem = gameListChallengeItem;

		String title = gameListChallengeItem.getOpponentUsername() + StaticData.SYMBOL_NEW_STR
				+ getString(R.string.win_) + StaticData.SYMBOL_SPACE + gameListChallengeItem.getOpponentWinCount()
				+ StaticData.SYMBOL_NEW_STR
				+ getString(R.string.loss_) + StaticData.SYMBOL_SPACE + gameListChallengeItem.getOpponentLossCount()
				+ StaticData.SYMBOL_NEW_STR
				+ getString(R.string.draw_) + StaticData.SYMBOL_SPACE + gameListChallengeItem.getOpponentDrawCount();

		popupItem.setPositiveBtnId(R.string.accept);
		popupItem.setNegativeBtnId(R.string.decline);
		showPopupDialog(title, CHALLENGE_ACCEPT_TAG);
	}

	private void updateGamesList() {
		if (!AppUtils.isNetworkAvailable(this)) {
			return;
		}

		LoadItem loadItem = new LoadItem();
		loadItem.setLoadPath(RestHelper.CMD_GAMES_ALL);
		loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, getAppData().getUserToken());
		new RequestJsonTask<DailyGamesAllItem>(dailyGamesUpdateListener).executeTask(loadItem);
	}

	private void loadDbGames() {
		new LoadDataFromDbTask(currentGamesCursorUpdateListener,
				DbHelper.getDailyCurrentMyListGamesParams(getCurrentUserName()),
				getContentResolver()).executeTask();
		new LoadDataFromDbTask(finishedGamesCursorUpdateListener,
				DbHelper.getDailyFinishedListGamesParams(getCurrentUserName()),
				getContentResolver()).executeTask();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				updateGamesList();
				updateVacationStatus();
				break;
//			case R.id.menu_new_game:
//				startActivity(new Intent(this, OnlineNewGameActivity.class));
//				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void releaseResources() {
		challengeInviteUpdateListener.releaseContext();
		challengeInviteUpdateListener = null;
		acceptDrawUpdateListener.releaseContext();
		acceptDrawUpdateListener = null;
		saveCurrentGamesListUpdateListener.releaseContext();
		saveCurrentGamesListUpdateListener = null;
		saveFinishedGamesListUpdateListener.releaseContext();
		saveFinishedGamesListUpdateListener = null;
		currentGamesCursorUpdateListener.releaseContext();
		currentGamesCursorUpdateListener = null;
		finishedGamesCursorUpdateListener.releaseContext();
		finishedGamesCursorUpdateListener = null;

		dailyGamesUpdateListener.releaseContext();
		dailyGamesUpdateListener = null;
		vacationDeleteUpdateListener.releaseContext();
		vacationDeleteUpdateListener = null;
		vacationGetUpdateListener.releaseContext();
		vacationGetUpdateListener = null;
	}

	private void showEmptyView(boolean show) {
		if (show) {
			emptyView.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
			loadingView.setVisibility(View.GONE);
		} else {
			emptyView.setVisibility(View.GONE);
			listView.setVisibility(View.VISIBLE);
		}
	}

	private void showLoadingView(boolean show) {
		if (show) {
			emptyView.setVisibility(View.GONE);
			if (sectionedAdapter.getCount() == 0) {
				listView.setVisibility(View.GONE);
				loadingView.setVisibility(View.VISIBLE);
			}
		} else {
			listView.setVisibility(View.VISIBLE);
			loadingView.setVisibility(View.GONE);
		}
	}

}