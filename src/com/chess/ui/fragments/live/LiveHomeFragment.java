package com.chess.ui.fragments.live;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.chess.R;
import com.chess.backend.LiveChessService;
import com.chess.backend.LoadHelper;
import com.chess.backend.LoadItem;
import com.chess.backend.entity.api.ServersStatsItem;
import com.chess.backend.tasks.RequestJsonTask;
import com.chess.lcc.android.DataNotValidException;
import com.chess.live.util.GameRatingClass;
import com.chess.statics.Symbol;
import com.chess.ui.adapters.ItemsAdapter;
import com.chess.ui.engine.SoundPlayer;
import com.chess.ui.engine.configs.LiveGameConfig;
import com.chess.ui.fragments.BasePopupsFragment;
import com.chess.ui.fragments.LiveBaseFragment;
import com.chess.ui.fragments.popup_fragments.PopupLiveTimeOptionsFragment;
import com.chess.ui.fragments.popup_fragments.PopupOptionsMenuFragment;
import com.chess.ui.fragments.stats.StatsGameDetailsFragment;
import com.chess.ui.fragments.stats.StatsGameFragment;
import com.chess.ui.interfaces.AbstractGameNetworkFaceHelper;
import com.chess.ui.interfaces.PopupListSelectionFace;
import com.chess.ui.views.chess_boards.ChessBoardBaseView;
import com.chess.ui.views.drawables.smart_button.ButtonDrawableBuilder;
import com.chess.utilities.LogMe;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 04.10.13
 * Time: 6:20
 */
public class LiveHomeFragment extends LiveBaseFragment implements PopupListSelectionFace, AdapterView.OnItemClickListener {

	private static final String OPTION_SELECTION_TAG = "time options popup";
	protected static final String FRIEND_SELECTION_TAG = "friend select popup";

	protected GameFaceHelper gameFaceHelper;
	protected Button timeSelectBtn;
	private PopupLiveTimeOptionsFragment timeOptionsFragment;
	private TimeOptionSelectedListener timeOptionSelectedListener;
	protected String[] newGameButtonsArray;
	protected TextView onlinePlayersCntTxt;
	protected List<LiveItem> featuresList;
	private ServerStatsUpdateListener serverStatsUpdateListener;
	private OptionsAdapter optionsAdapter;
	private LiveItem currentGameItem;
	protected PopupOptionsMenuFragment friendSelectFragment;
	protected FriendSelectedListener friendSelectedListener;
	protected String[] liveFriends;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		currentGameItem = new LiveItem(R.string.ic_live_standard, R.string.current_game);
		featuresList = new ArrayList<LiveItem>();
		featuresList.add(new LiveItem(R.string.ic_binoculars, R.string.top_game));
		featuresList.add(new LiveItem(R.string.ic_stats, R.string.stats));
		featuresList.add(new LiveItem(R.string.ic_challenge_friend, R.string.friends));
		featuresList.add(new LiveItem(R.string.ic_board, R.string.archive));
		optionsAdapter = new OptionsAdapter(getActivity(), featuresList);

		gameFaceHelper = new GameFaceHelper();
		timeOptionSelectedListener = new TimeOptionSelectedListener();
		friendSelectedListener = new FriendSelectedListener();
		serverStatsUpdateListener = new ServerStatsUpdateListener();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_list_view_frame, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setTitle(R.string.live);

		getAppData().setLiveChessMode(true); // we should set it before parent call to update isLCSBound flag

		widgetsInit(view);
	}

	@Override
	public void onResume() {
		super.onResume();

		//LogMe.dl("LCCLOG", "LiveHomeFragment onResume isNetworkAvailable() " + isNetworkAvailable());

		try {

			if (!isNetworkAvailable()) {
				// ask user to turn device connection on, prevent showing several instances of "connection settings"
				/*
				popupItem.setPositiveBtnId(R.string.check_connection);
				showPopupDialog(R.string.no_network, NETWORK_CHECK_TAG);
				*/
			}

			if (!isLCSBound) {
				liveBaseActivity.connectLcc();
				showPopupProgressDialog();
			} else {
				addCurrentGameItem(getLiveService());
			}

		} catch (DataNotValidException e) {
			e.printStackTrace();
		}

		if (isNetworkAvailable()) {
			// get online players count
			LoadItem loadItem = LoadHelper.getServerStats();
			new RequestJsonTask<ServersStatsItem>(serverStatsUpdateListener).executeTask(loadItem);
		}
	}

	/*
	@Override
	public void onPause() {
		super.onPause();

		liveBaseActivity.stopConnectTimer();
	}
	*/

	protected void addCurrentGameItem(LiveChessService liveService) {
		if (liveService.isActiveGamePresent() && !liveService.getCurrentGame().isTopObserved()) {
			if (!featuresList.contains(currentGameItem)) {
				featuresList.add(0, currentGameItem);
				optionsAdapter.notifyDataSetChanged();
			}
		} else {
			featuresList.remove(currentGameItem);
		}
	}

	private class ServerStatsUpdateListener extends ChessUpdateListener<ServersStatsItem> {
		private ServerStatsUpdateListener() {
			super(ServersStatsItem.class);
		}

		@Override
		public void updateData(ServersStatsItem returnedObj) {
			super.updateData(returnedObj);

			long cnt = returnedObj.getData().getTotals().getLive();
			String playersOnlineStr = NumberFormat.getInstance().format(cnt);

			onlinePlayersCntTxt.setText(getString(R.string.players_online_arg, playersOnlineStr));
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		LiveItem liveItem = (LiveItem) parent.getItemAtPosition(position);

		if (liveItem == null) {
			return;
		}

		if (liveItem.iconId == R.string.ic_binoculars) {  // Top Game
			if (!isLCSBound) {
				showToast(R.string.not_connected_yet);
				return;
			}
			try { // check for valid data
				 getLiveService();
			} catch (DataNotValidException e) {
				e.printStackTrace();
				showToast(R.string.not_connected_yet);
				return;
			}

			Fragment fragmentByTag;
			if (!isTablet) {
				fragmentByTag = getFragmentManager().findFragmentByTag(GameLiveObserveFragment.class.getSimpleName());
				if (fragmentByTag == null) {
					fragmentByTag = new GameLiveObserveFragment();
				}
			}else {
				fragmentByTag = getFragmentManager().findFragmentByTag(GameLiveObserveFragmentTablet.class.getSimpleName());
				if (fragmentByTag == null) {
					fragmentByTag = new GameLiveObserveFragmentTablet();
				}
			}
			getActivityFace().openFragment((BasePopupsFragment) fragmentByTag);
		} else if (liveItem.iconId == R.string.ic_live_standard) { // Current game
			Fragment fragmentByTag;
			if (!isTablet) {
				fragmentByTag = getFragmentManager().findFragmentByTag(GameLiveFragment.class.getSimpleName());
			} else {
				fragmentByTag = getFragmentManager().findFragmentByTag(GameLiveFragmentTablet.class.getSimpleName());
			}
			if (fragmentByTag == null) {
				if (!isTablet) {
					fragmentByTag = new GameLiveFragment();
				} else {
					fragmentByTag = new GameLiveFragmentTablet();
				}
			}
			getActivityFace().openFragment((BasePopupsFragment) fragmentByTag);
		} else if (liveItem.iconId == R.string.ic_stats) { // Stats
			getActivityFace().openFragment(StatsGameDetailsFragment.createInstance(
					StatsGameFragment.LIVE_STANDARD, true, getUsername()));
		} else if (liveItem.iconId == R.string.ic_challenge_friend) { // Friends
			if (isLCSBound) {
				try {
					LiveChessService liveService = getLiveService();
					liveFriends = liveService.getOnlineFriends();

					if (liveFriends == null || liveFriends.length == 0) {
						showToast(R.string.no_friends_online);
						return;
					}

					SparseArray<String> optionsMap = new SparseArray<String>();
					for (int i = 0; i < liveFriends.length; i++) {
						String friend = liveFriends[i];
						optionsMap.put(i, friend);
					}

					friendSelectFragment = PopupOptionsMenuFragment.createInstance(friendSelectedListener, optionsMap);
					friendSelectFragment.show(getFragmentManager(), FRIEND_SELECTION_TAG);
				} catch (DataNotValidException e) {
					e.printStackTrace();
				}
			}
		} else if (liveItem.iconId == R.string.ic_board) { // Archive
			getActivityFace().openFragment(new LiveGamesArchiveFragment());
		}
	}

	@Override
	public void onClick(View view) {
		super.onClick(view);

		if (view.getId() == R.id.timeSelectBtn) {
			// show popup
			if (timeOptionsFragment != null) {
				return;
			}

			timeOptionsFragment = PopupLiveTimeOptionsFragment.createInstance(timeOptionSelectedListener);
			timeOptionsFragment.show(getFragmentManager(), OPTION_SELECTION_TAG);
		} else if (view.getId() == R.id.gamePlayBtn) {
			createLiveChallenge();
		} else if (view.getId() == R.id.newGameHeaderView) {
			getActivityFace().changeRightFragment(LiveGameOptionsFragment.createInstance(CENTER_MODE));
			getActivityFace().toggleRightMenu();
		}
	}

	@Override
	public void onValueSelected(int code) {
		setDefaultTimeMode(code);

		timeOptionsFragment.dismiss();
		timeOptionsFragment = null;
	}

	@Override
	public void onDialogCanceled() {
		timeOptionsFragment = null;
	}

	private void createLiveChallenge() {
		LiveGameConfig.Builder gameConfigBuilder = getAppData().getLiveGameConfigBuilder();
		int minRating = gameConfigBuilder.getMinRating();
		int maxRating = gameConfigBuilder.getMaxRating();
		if (minRating == 0 || maxRating == 0) {
			try {
				LiveChessService liveService = getLiveService();
				Integer standardRating = liveService.getUser().getRatingFor(GameRatingClass.Standard);
				Integer blitzRating = liveService.getUser().getRatingFor(GameRatingClass.Blitz);
				Integer lightningRating = liveService.getUser().getRatingFor(GameRatingClass.Lightning);


				if (gameConfigBuilder.getTimeMode() == LiveGameConfig.STANDARD) {
					minRating = standardRating - LiveGameConfig.RATING_STEP;
					maxRating = standardRating + LiveGameConfig.RATING_STEP;
				} else if (gameConfigBuilder.getTimeMode() == LiveGameConfig.BLITZ) {
					minRating = blitzRating - LiveGameConfig.RATING_STEP;
					maxRating = blitzRating + LiveGameConfig.RATING_STEP;
				} else if (gameConfigBuilder.getTimeMode() == LiveGameConfig.LIGHTNING) {
					minRating = lightningRating - LiveGameConfig.RATING_STEP;
					maxRating = lightningRating + LiveGameConfig.RATING_STEP;
				}

				gameConfigBuilder.setMinRating(minRating);
				gameConfigBuilder.setMaxRating(maxRating);

				// save config
				getAppData().setLiveGameConfigBuilder(gameConfigBuilder);
			} catch (DataNotValidException e) {
				e.printStackTrace();
				return;
			}
		}

		getActivityFace().openFragment(LiveGameWaitFragment.createInstance(gameConfigBuilder.build()));
	}

	@Override
	public void onLiveClientConnected() {
		super.onLiveClientConnected();
		dismissProgressDialog();
	}

	@Override
	public void startGameFromService() {
		LogMe.dl("lcc", "startGameFromService");

		final FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					LiveChessService liveService;
					try {
						liveService = getLiveService();
					} catch (DataNotValidException e) {
						logTest(e.getMessage());
						showToast(e.getMessage());
						return;
					}
					logTest("challenge created, ready to start");

					Long gameId = liveService.getCurrentGameId();
					logTest("gameId = " + gameId);
					getActivityFace().openFragment(GameLiveFragment.createInstance(gameId));
				}
			});
		}
	}

	protected void widgetsInit(View view) {
		Resources resources = getResources();
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		ViewGroup headerView = (ViewGroup) inflater.inflate(R.layout.new_play_home_header_frame, null, false);

		inviteOverlaySetup(resources,  headerView.findViewById(R.id.startOverlayView), resources.getDisplayMetrics().widthPixels / 8); // one square size);
		onlinePlayersCntTxt = (TextView) headerView.findViewById(R.id.onlinePlayersCntTxt);

		headerView.findViewById(R.id.newGameHeaderView).setOnClickListener(this);
		headerView.findViewById(R.id.gamePlayBtn).setOnClickListener(this);

		{ // Time mode adjustments
			int mode = getAppData().getDefaultLiveMode();
			// set texts to buttons
			newGameButtonsArray = getResources().getStringArray(R.array.new_live_game_button_values);
			// TODO add sliding from outside animation for time modes in popup
			timeSelectBtn = (Button) headerView.findViewById(R.id.timeSelectBtn);
			timeSelectBtn.setOnClickListener(this);
			timeSelectBtn.setText(getLiveModeButtonLabel(newGameButtonsArray[mode]));
		}

		ListView listView = (ListView) view.findViewById(R.id.listView);
		listView.addHeaderView(headerView);
		listView.setAdapter(optionsAdapter);
		listView.setOnItemClickListener(this);

		ChessBoardBaseView boardView = (ChessBoardBaseView) headerView.findViewById(R.id.boardview);
		boardView.setGameFace(gameFaceHelper);
	}

	protected void inviteOverlaySetup(Resources resources, View startOverlayView, int squareSize) {
		// let's make it to match board properties
		// it should be 2 squares inset from top of border and 4 squares tall + 1 squares from sides
		int borderOffset = resources.getDimensionPixelSize(R.dimen.invite_overlay_top_offset);
		// now we add few pixel to compensate shadow addition
		int shadowOffset = resources.getDimensionPixelSize(R.dimen.overlay_shadow_offset);
		borderOffset += shadowOffset;
		int overlayHeight = squareSize * 4 + borderOffset + shadowOffset;
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				overlayHeight);
		int topMargin = squareSize * 2 + borderOffset - shadowOffset * 2;

		params.setMargins(squareSize - borderOffset, topMargin, squareSize - borderOffset, 0);
		params.addRule(RelativeLayout.ALIGN_TOP, R.id.boardView);
		startOverlayView.setLayoutParams(params);
		startOverlayView.setVisibility(View.VISIBLE);
	}

	protected static class LiveItem {
		int iconId;
		int labelId;

		private LiveItem(int iconId, int labelId) {
			this.iconId = iconId;
			this.labelId = labelId;
		}
	}

	private class OptionsAdapter extends ItemsAdapter<LiveItem> {

		private final int sidePadding;
		private final int whiteColor;

		public OptionsAdapter(Context context, List<LiveItem> itemList) {
			super(context, itemList);
			sidePadding = (int) (8 * density);
			whiteColor = resources.getColor(R.color.white);
		}

		@Override
		protected View createView(ViewGroup parent) {
			View view = inflater.inflate(R.layout.new_dark_spinner_item, parent, false);

			ButtonDrawableBuilder.setBackgroundToView(view, R.style.ListItem_Header_Dark);
			view.setPadding(sidePadding, 0, sidePadding, 0);

			ViewHolder holder = new ViewHolder();
			holder.nameTxt = (TextView) view.findViewById(R.id.categoryNameTxt);
			holder.iconTxt = (TextView) view.findViewById(R.id.iconTxt);
			holder.spinnerIcon = (TextView) view.findViewById(R.id.spinnerIcon);
			holder.spinnerIcon.setVisibility(View.GONE);

			holder.nameTxt.setPadding(sidePadding, 0, 0, 0);
			holder.nameTxt.setTextColor(whiteColor);
			holder.iconTxt.setVisibility(View.VISIBLE);

			view.setTag(holder);
			return view;
		}

		@Override
		protected void bindView(LiveItem item, int pos, View convertView) {
			ViewHolder holder = (ViewHolder) convertView.getTag();


			holder.nameTxt.setText(item.labelId);
			holder.iconTxt.setText(item.iconId);
		}

		private class ViewHolder {
			TextView iconTxt;
			TextView nameTxt;
			TextView spinnerIcon;
		}
	}

	private class GameFaceHelper extends AbstractGameNetworkFaceHelper {

		@Override
		public SoundPlayer getSoundPlayer() {
			return SoundPlayer.getInstance(getActivity());
		}

		@Override
		public boolean isAlive() {
			return getActivity() != null;
		}
	}

	private class TimeOptionSelectedListener implements PopupListSelectionFace {

		@Override
		public void onValueSelected(int code) {
			timeOptionsFragment.dismiss();
			timeOptionsFragment = null;

			setDefaultTimeMode(code);
		}

		@Override
		public void onDialogCanceled() {
			timeOptionsFragment = null;
		}
	}

	private class FriendSelectedListener implements PopupListSelectionFace {

		@Override
		public void onValueSelected(int code) {
			friendSelectFragment.dismiss();
			friendSelectFragment = null;

			String friend = liveFriends[code];

			getActivityFace().changeRightFragment(LiveGameOptionsFragment.createInstance(RIGHT_MENU_MODE, friend));
			getActivityFace().toggleRightMenu();
		}

		@Override
		public void onDialogCanceled() {
			friendSelectFragment = null;
		}
	}

	protected String getLiveModeButtonLabel(String label) {
		if (label.contains(Symbol.SLASH)) { // like "5 | 2"
			return label;
		} else { // "10 min"
			return getString(R.string.min_arg, label);
		}
	}

	private void setDefaultTimeMode(int mode) {
		timeSelectBtn.setText(getLiveModeButtonLabel(newGameButtonsArray[mode]));
		getAppData().setDefaultLiveMode(mode);
	}

	/*
	@Override
	public void onPositiveBtnClick(DialogFragment fragment) {
		String tag = fragment.getTag();
		if (tag == null) {
			super.onPositiveBtnClick(fragment);
			return;
		}

		if (tag.equals(NETWORK_CHECK_TAG)) {
			startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), NETWORK_REQUEST);
		}
		super.onPositiveBtnClick(fragment);
	}
	*/
}
