package com.chess.ui.activities;

import actionbarcompat.ActionBarActivityHome;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.chess.R;
import com.chess.backend.LiveChessService;
import com.chess.backend.RestHelper;
import com.chess.backend.interfaces.AbstractUpdateListener;
import com.chess.backend.statics.AppConstants;
import com.chess.backend.statics.AppData;
import com.chess.backend.statics.StaticData;
import com.chess.backend.tasks.CheckUpdateTask;
import com.chess.lcc.android.LiveEvent;
import com.chess.lcc.android.OuterChallengeListener;
import com.chess.lcc.android.interfaces.LccConnectionUpdateFace;
import com.chess.lcc.android.interfaces.LiveChessClientEventListener;
import com.chess.live.client.Challenge;
import com.chess.live.util.GameTimeConfig;
import com.chess.model.PopupItem;
import com.chess.ui.fragments.PopupCustomViewFragment;
import com.chess.ui.fragments.PopupDialogFragment;
import com.chess.ui.interfaces.PopupDialogFace;
import com.chess.utilities.AppUtils;
import com.chess.utilities.InneractiveAdHelper;
import com.facebook.android.Facebook;
import com.facebook.android.LoginButton;
import com.inneractive.api.ads.InneractiveAd;

import java.util.Map;

/**
 * HomeScreenActivity class
 *
 * @author alien_roger
 * @created at: 08.02.12 6:29
 */
public class HomeScreenActivity extends ActionBarActivityHome implements PopupDialogFace,
		LiveChessClientEventListener, View.OnClickListener {

	private static final String TAG = "LccLog-HomeScreenActivity";
	private static final String CONNECT_FAILED_TAG = "connect_failed";
	public static final String OBSOLETE_VERSION_TAG = "obsolete version";
	protected static final String CHALLENGE_TAG = "challenge_tag";
	protected static final String LOGOUT_TAG = "logout_tag";

	private InneractiveAd inneractiveFullscreenAd;
	//protected MoPubInterstitial moPubInterstitial;
	private boolean forceFlag;
	private LiveOuterChallengeListener outerChallengeListener;
	protected ChallengeTaskListener challengeTaskListener;
	private LiveServiceConnectionListener liveServiceConnectionListener;
	private boolean isLCSBound;

	protected Challenge currentChallenge;
	private LiveChessService liveService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.home_screen);
		AppUtils.setBackground(findViewById(R.id.mainView), this);

		findViewById(R.id.playLiveFrame).setOnClickListener(this);
		findViewById(R.id.playOnlineFrame).setOnClickListener(this);
		findViewById(R.id.playComputerFrame).setOnClickListener(this);
		findViewById(R.id.tacticsTrainerFrame).setOnClickListener(this);
		findViewById(R.id.videoLessonsFrame).setOnClickListener(this);
		findViewById(R.id.settingsFrame).setOnClickListener(this);

		outerChallengeListener = new LiveOuterChallengeListener();
		challengeTaskListener = new ChallengeTaskListener();
		liveServiceConnectionListener = new LiveServiceConnectionListener();

		registerGcmService();
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (AppData.isLiveChess(this)) { // bound only if we really need it
			Log.d("TEST", "onStart isLCSBound = " + isLCSBound + " in " + HomeScreenActivity.this);
			if (isLCSBound) {
				onLiveServiceConnected();
			} else {
				bindService(new Intent(this, LiveChessService.class), liveServiceConnectionListener, BIND_AUTO_CREATE);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		long startDay = preferences.getLong(AppConstants.START_DAY, 0);
		Log.d("CheckUpdateTask", "startDay loaded, = " + startDay);

		if (startDay == 0 || !DateUtils.isToday(startDay)) {
			checkUpdate();
		}

		if (isLCSBound) {
			executePausedActivityLiveEvents();
		}

		showFullScreenAd();
		adjustActionBar();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Bundle extras = intent.getExtras();
		if(extras != null){
			int cmd = extras.getInt(StaticData.NAVIGATION_CMD);
			if(cmd == StaticData.NAV_FINISH_2_LOGIN){
				Intent loginIntent = new Intent(this, LoginScreenActivity.class);
				loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(loginIntent);
				finish();
				extras.clear();
			} else if(cmd == StaticData.NAV_FINISH_2_SPLASH){
				Intent loginIntent = new Intent(this, SplashActivity.class);
				loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(loginIntent);
				finish();
				extras.clear();
			}
		}
	}
	

	public void executePausedActivityLiveEvents() {

		Map<LiveEvent.Event, LiveEvent> pausedActivityLiveEvents = liveService.getPausedActivityLiveEvents();
		Log.d(TAG, "executePausedActivityLiveEvents size=" + pausedActivityLiveEvents.size() + ", events=" + pausedActivityLiveEvents);

		if (pausedActivityLiveEvents.size() > 0) {
			LiveEvent connectionFailureEvent = pausedActivityLiveEvents.get(LiveEvent.Event.CONNECTION_FAILURE);
			if (connectionFailureEvent != null) {
				pausedActivityLiveEvents.remove(LiveEvent.Event.CONNECTION_FAILURE);
				processConnectionFailure(connectionFailureEvent.getMessage());
			}

			LiveEvent challengeEvent = pausedActivityLiveEvents.get(LiveEvent.Event.CHALLENGE);
			if (challengeEvent != null) {
				pausedActivityLiveEvents.remove(LiveEvent.Event.CHALLENGE);
				if (challengeEvent.isChallengeDelayed()) {
					outerChallengeListener.showDelayedDialogImmediately(challengeEvent.getChallenge());
				} else {
					outerChallengeListener.showDialogImmediately(challengeEvent.getChallenge());
				}
			}
		}
	}

	/*@Override
	protected void onPause() {
		super.onPause();

		preferencesEditor.putLong(AppConstants.LAST_ACTIVITY_PAUSED_TIME, System.currentTimeMillis());
		preferencesEditor.commit();

		//mainApp.setForceBannerAdOnFailedLoad(false);
	}*/

	private class LiveServiceConnectionListener implements ServiceConnection, LccConnectionUpdateFace {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

			Log.d(TAG, "onLiveServiceConnected");
			LiveChessService.ServiceBinder serviceBinder = (LiveChessService.ServiceBinder) iBinder;
			liveService = serviceBinder.getService();
			isLCSBound = true;
			Log.d("lcc", "SERVICE: HOME onLiveServiceConnected, liveService = " + liveService);
			liveService.setLiveChessClientEventListener(HomeScreenActivity.this);

			liveService.checkAndConnect(this);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			Log.d("lcc", "SERVICE: onServiceDisconnected");
			isLCSBound = false;
		}

		@Override
		public void onConnected() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onLiveServiceConnected();
				}
			});
		}
	}

	private void onLiveServiceConnected() {
		Log.d(TAG, "onLiveServiceConnected callback, liveService.isConnected() = " + liveService.isConnected());
		getActionBarHelper().showMenuItemById(R.id.menu_signOut, liveService.isConnected());

		liveService.setOuterChallengeListener(outerChallengeListener);
		liveService.setChallengeTaskListener(challengeTaskListener);

		//executePausedActivityLiveEvents();
	}

	@Override
	public void onPositiveBtnClick(DialogFragment fragment) {
		String tag = fragment.getTag();
		if (tag == null) {
			super.onPositiveBtnClick(fragment);
			return;
		}

		if (tag.equals(CONNECT_FAILED_TAG)) {
			if (AppData.isLiveChess(this)) {
				liveService.logout();
			}
		}
		 else if (tag.equals(OBSOLETE_VERSION_TAG)) {
			// Show site and
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AppData.setLiveChessMode(getContext(), false);
					liveService.setConnected(false);
					startActivity(new Intent(Intent.ACTION_VIEW, Uri
							.parse(RestHelper.PLAY_ANDROID_HTML)));
				}
			});
		}
		if (tag.equals(CHECK_UPDATE_TAG)) {
			if (forceFlag) {
				// drop start day
				preferencesEditor.putLong(AppConstants.START_DAY, 0);
				preferencesEditor.commit();

				backToLoginActivity();
			}
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(RestHelper.GOOGLE_PLAY_URI));
			startActivity(intent);
		}
		else if (tag.equals(LOGOUT_TAG)) {
			if (isLCSBound) {
				liveService.logout();
			}
			getActionBarHelper().showMenuItemById(R.id.menu_signOut, false);
		}else if(tag.equals(CHALLENGE_TAG)){
			if (isLCSBound) {
				Log.i(TAG, "Accept challenge: " + currentChallenge);
				liveService.runAcceptChallengeTask(currentChallenge);
				liveService.declineAllChallenges(currentChallenge);
			}
		}

		super.onPositiveBtnClick(fragment);
	}

	@Override
	public void onNegativeBtnClick(DialogFragment fragment) {
		String tag = fragment.getTag();
		if (tag == null) {
			super.onNegativeBtnClick(fragment);
			return;
		}

		if (tag.equals(CHALLENGE_TAG)) {
			fragment.dismiss();

			if (isLCSBound) {
				// todo: refactor with new LCC
				if (!liveService.isConnected() || liveService.getClient() == null) { // TODO should leave that screen on connection lost or when LCC is become null
					liveService.logout();
					backToHomeActivity();
					return;
				}

				// todo: check decline challenge
				Log.i(TAG, "Decline challenge: " + currentChallenge);
				popupManager.remove(fragment);
				liveService.declineCurrentChallenge(currentChallenge);
			}

		}
		super.onNegativeBtnClick(fragment);
	}

	private void adjustActionBar(){
		boolean isConnected = false;
		if (isLCSBound) {
			isConnected = liveService.isConnected();
		}

		getActionBarHelper().showMenuItemById(R.id.menu_signOut, isConnected);

		getActionBarHelper().showMenuItemById(R.id.menu_search, false);
		getActionBarHelper().showMenuItemById(R.id.menu_settings, false);
		getActionBarHelper().showMenuItemById(R.id.menu_new_game, false);
		getActionBarHelper().showMenuItemById(R.id.menu_refresh, false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.sign_out, menu);
		boolean isConnected = false;
		if (isLCSBound) {
			isConnected = liveService.isConnected();
		}

		getActionBarHelper().showMenuItemById(R.id.menu_signOut, isConnected, menu);

		getActionBarHelper().showMenuItemById(R.id.menu_search, false, menu);
		getActionBarHelper().showMenuItemById(R.id.menu_settings, false, menu);
		getActionBarHelper().showMenuItemById(R.id.menu_refresh, false, menu);
		getActionBarHelper().showMenuItemById(R.id.menu_new_game, false, menu);
		return super.onCreateOptionsMenu(menu);
	}

	private class ChallengeTaskListener extends AbstractUpdateListener<Challenge> {
		public ChallengeTaskListener() {
			super(getContext());
		}
	}

	// ---------- LiveChessClientEventListener ----------------
	@Override
	public void onConnecting() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getActionBarHelper().showMenuItemById(R.id.menu_signOut, false);
				getActionBarHelper().setRefreshActionItemState(true);
			}
		});
	}

	@Override
	public void onConnectionEstablished() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getActionBarHelper().setRefreshActionItemState(false);
				getActionBarHelper().showMenuItemById(R.id.menu_signOut, true);
			}
		});
	}

	@Override
	public void onSessionExpired(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				final LinearLayout customView = (LinearLayout) inflater.inflate(R.layout.popup_relogin_frame, null, false);

				PopupItem popupItem = new PopupItem();
				popupItem.setCustomView(customView);

				PopupCustomViewFragment reLoginFragment = PopupCustomViewFragment.newInstance(popupItem);
				reLoginFragment.show(getSupportFragmentManager(), RE_LOGIN_TAG);

				liveService.logout();

				((TextView) customView.findViewById(R.id.titleTxt)).setText(message);

				EditText usernameEdt = (EditText) customView.findViewById(R.id.usernameEdt);
				EditText passwordEdt = (EditText) customView.findViewById(R.id.passwordEdt);
				setLoginFields(usernameEdt, passwordEdt);

				customView.findViewById(R.id.re_signin).setOnClickListener(HomeScreenActivity.this);

				LoginButton facebookLoginButton = (LoginButton) customView.findViewById(R.id.re_fb_connect);
                facebookInit(facebookLoginButton);
				facebookLoginButton.logout();

				usernameEdt.setText(AppData.getUserName(HomeScreenActivity.this));
			}
		});
	}

	@Override
	public void onConnectionFailure(String message) {
		if (isPaused) {
			LiveEvent connectionFailureEvent = new LiveEvent();
			connectionFailureEvent.setEvent(LiveEvent.Event.CONNECTION_FAILURE);
			connectionFailureEvent.setMessage(message);
			liveService.getPausedActivityLiveEvents().put(connectionFailureEvent.getEvent(), connectionFailureEvent);
		} else {
			processConnectionFailure(message);
		}
	}

	private void processConnectionFailure(String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getActionBarHelper().setRefreshActionItemState(false);
				getActionBarHelper().showMenuItemById(R.id.menu_signOut, false);
			}
		});

		showPopupDialog(R.string.error, message, CONNECT_FAILED_TAG, 1);
		getLastPopupFragment().setCancelable(false);
	}

    @Override
    public void onConnectionBlocked(boolean blocked) {
    }

    @Override
	public void onObsoleteProtocolVersion() {
		popupItem.setButtons(1);
		showPopupDialog(R.string.version_check, R.string.version_is_obsolete_update, OBSOLETE_VERSION_TAG);
		getLastPopupFragment().setCancelable(false);
	}

	@Override
	public void onFriendsStatusChanged(){
	}

	@Override
	public void onAdminAnnounce(String message) {
		showSinglePopupDialog(message);
	}

	// -----------------------------------------------------


	private void checkUpdate() {
		new CheckUpdateTask(new CheckUpdateListener()).executeTask(RestHelper.GET_ANDROID_VERSION);
	}

	private class CheckUpdateListener extends AbstractUpdateListener<Boolean> {
		public CheckUpdateListener() {
			super(getContext());
		}

		@Override
		public void showProgress(boolean show) {
		}

		@Override
		public void updateData(Boolean returnedObj) {
			forceFlag = returnedObj;
			if (isPaused)
				return;

			popupItem.setButtons(1);
			showPopupDialog(R.string.update_check, R.string.update_available_please_update, CHECK_UPDATE_TAG);
		}
	}

	@Override
	protected void backToLoginActivity() {
		Intent intent = new Intent(this, LoginScreenActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
	}

	/*@Override
	public void onClick(View view) {
		if(view.getId() == R.id.re_signin){
			signInUser();
		}
	}*/

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK && requestCode == Facebook.DEFAULT_AUTH_ACTIVITY_CODE){
			facebook.authorizeCallback(requestCode, resultCode, data);
		}
	}

//	public LccHelper getLccHolder() { // todo: check null
//		return liveService == null ? null : liveService.getLccHolder();
//	}

	private class LiveOuterChallengeListener implements OuterChallengeListener {
		@Override
		public void showDelayedDialog(Challenge challenge) {
			if (isPaused) {
				LiveEvent liveEvent = new LiveEvent();
				liveEvent.setEvent(LiveEvent.Event.CHALLENGE);
				liveEvent.setChallenge(challenge);
				liveEvent.setChallengeDelayed(true);
				liveService.getPausedActivityLiveEvents().put(liveEvent.getEvent(), liveEvent);
			} else {
				showDelayedDialogImmediately(challenge);
			}
		}

		@Override
		public void showDelayedDialogImmediately(Challenge challenge) {
			Log.d(TAG, "CHALLENGE showDelayedDialogImmediately -> popupDialogFragment.show ");
			currentChallenge = challenge;
			popupItem.setPositiveBtnId(R.string.accept);
			popupItem.setNegativeBtnId(R.string.decline);
			showPopupDialog(R.string.you_been_challenged, composeMessage(challenge), CHALLENGE_TAG);
		}

		@Override
		public void showDialogImmediately(Challenge challenge) {
			if (popupManager.size() > 0) {
				return;
			}

			currentChallenge = challenge;

			PopupItem popupItem = new PopupItem();
			popupItem.setTitle(R.string.you_been_challenged);
			popupItem.setMessage(composeMessage(challenge));
			popupItem.setNegativeBtnId(R.string.decline);
			popupItem.setPositiveBtnId(R.string.accept);

			Log.d(TAG, "CHALLENGE showDialogImmediately -> popupDialogFragment.show ");
			PopupDialogFragment popupDialogFragment = PopupDialogFragment.newInstance(popupItem);
			popupDialogFragment.show(getSupportFragmentManager(), CHALLENGE_TAG);

			popupManager.add(popupDialogFragment);
		}

		@Override
		public void showDialog(Challenge challenge) {
			if (isPaused) {
				LiveEvent liveEvent = new LiveEvent();
				liveEvent.setEvent(LiveEvent.Event.CHALLENGE);
				liveEvent.setChallenge(challenge);
				liveEvent.setChallengeDelayed(false);
				liveService.getPausedActivityLiveEvents().put(liveEvent.getEvent(), liveEvent);
			} else {
				showDialogImmediately(challenge);
			}
		}

		@Override
		public void hidePopups() {
			dismissAllPopups();
		}

		private String composeMessage(Challenge challenge){
			String rated = challenge.isRated()? getString(R.string.rated): getString(R.string.unrated);
			GameTimeConfig config = challenge.getGameTimeConfig();
			String blitz = StaticData.SYMBOL_EMPTY;
			if(config.isBlitz()){
				blitz = getString(R.string.blitz_game);
			}else if(config.isLightning()){
				blitz = getString(R.string.lightning_game);
			}else if(config.isStandard()){
				blitz = getString(R.string.standard_game);
			}

			String timeIncrement = StaticData.SYMBOL_EMPTY;

			if(config.getTimeIncrement() > 0){
				timeIncrement = " | "+ String.valueOf(config.getTimeIncrement()/10);
			}

			String timeMode = config.getBaseTime()/10/60 + timeIncrement + StaticData.SYMBOL_SPACE + blitz;
			String playerColor;

			switch (challenge.getColor()) {
				case UNDEFINED:
					playerColor = getString(R.string.random);
					break;
				case WHITE:
					playerColor = getString(R.string.black);
					break;
				case BLACK:
					playerColor = getString(R.string.white);
					break;
				default:
					playerColor = getString(R.string.random);
					break;
			}

			return new StringBuilder()
					.append(getString(R.string.opponent_)).append(StaticData.SYMBOL_SPACE)
					.append(challenge.getFrom().getUsername())
					.append(getString(R.string.time_)).append(StaticData.SYMBOL_SPACE)
					.append(timeMode)
					.append(getString(R.string.you_play)).append(StaticData.SYMBOL_SPACE)
					.append(playerColor).append(StaticData.SYMBOL_NEW_STR)
					.append(rated)
					.toString();
		}
	}


	private void showFullScreenAd() {
		if (!preferences.getBoolean(AppConstants.FULLSCREEN_AD_ALREADY_SHOWED, false) && AppUtils.isNeedToUpgrade(this)) {

			// TODO handle for support show ad on tablet in portrait mode
			// TODO: add support for tablet ad units

			if (InneractiveAdHelper.IS_SHOW_FULLSCREEN_ADS) {

				  /*moPubInterstitial = new MoPubInterstitial(this, "agltb3B1Yi1pbmNyDQsSBFNpdGUYwLyBEww"); // chess.com
				//moPubInterstitial = new MoPubInterstitial(this, "12345"); // test
				//moPubInterstitial = new MoPubInterstitial(this, "agltb3B1Yi1pbmNyDAsSBFNpdGUYsckMDA"); // test
				moPubInterstitial.setListener(this);
				moPubInterstitial.load();*/

				InneractiveAdHelper.InneractiveAdListenerImpl adListener =
						new InneractiveAdHelper.InneractiveAdListenerImpl(AppConstants.AD_FULLSCREEN, preferencesEditor);
				String inneractiveAdsAppId = getString(R.string.inneractiveAdsAppId);
				inneractiveFullscreenAd = new InneractiveAd(this, inneractiveAdsAppId, InneractiveAd.IaAdType.Interstitial, 0);
				inneractiveFullscreenAd.setInneractiveListener(adListener);
				((LinearLayout) findViewById(R.id.mainView)).addView(inneractiveFullscreenAd);
			}
		}
	}

	@Override
	protected void onDestroy() {
		if (inneractiveFullscreenAd != null) {
			inneractiveFullscreenAd.cleanUp();
		}
		super.onDestroy();
		if (isLCSBound) {
			unbindService(liveServiceConnectionListener);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_signOut:
				showPopupDialog(R.string.confirm, R.string.signout_confirm, LOGOUT_TAG);
				break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.playLiveFrame) {
			Class<?> clazz = AppData.isGuest(this) ? SignUpScreenActivity.class : LiveScreenActivity.class;
			startAnimatedActivity(view, clazz);

		} else if (view.getId() == R.id.playOnlineFrame) {
			Class<?> clazz = AppData.isGuest(this) ? SignUpScreenActivity.class : OnlineScreenActivity.class;
			startAnimatedActivity(view, clazz);

		} else if (view.getId() == R.id.playComputerFrame) {
			startAnimatedActivity(view, ComputerScreenActivity.class);

		} else if (view.getId() == R.id.tacticsTrainerFrame) {
			startAnimatedActivity(view, GameTacticsScreenActivity.class);

		} else if (view.getId() == R.id.videoLessonsFrame) {
			startAnimatedActivity(view, VideoScreenActivity.class);

		} else if (view.getId() == R.id.settingsFrame) {
			startAnimatedActivity(view, PreferencesScreenActivity.class);
		}
	}

	private void startAnimatedActivity(View view, Class clazz) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			// Create a scale-up animation that originates at the button being pressed.

			ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.getWidth()/2, view.getHeight()/2);
			// Request the activity be started, using the custom animation options.
			startActivity(new Intent(this, clazz), opts.toBundle());

		} else {
			startActivity(new Intent(this, clazz));
		}
	}

	 /*public void OnInterstitialLoaded() {
    if (moPubInterstitial.isReady()) {
      Log.d(AdView.MOPUB, "interstitial ad listener: loaded and ready");
      moPubInterstitial.show();

      preferencesEditor.putBoolean(AppConstants.FULLSCREEN_AD_ALREADY_SHOWED, true);
      preferencesEditor.commit();
    }
    else {
      Log.d(AdView.MOPUB, "interstitial ad listener: loaded, but not ready");
    }

    String response = moPubInterstitial.getMoPubInterstitialView().getResponseString();
    if (response != null && response.contains(AppConstants.MATOMY_AD)) {
      Map<String, String> params = new HashMap<String, String>();
      params.put(AppConstants.RESPONSE, response);
      FlurryAgent.logEvent(FlurryData.MATOMY_AD_FULLSCREEN_LOADED, params);
    }
  }

  public void OnInterstitialFailed() {
    Log.d(AdView.MOPUB, "interstitial ad listener: failed");

    String response = moPubInterstitial.getMoPubInterstitialView().getResponseString();
    if (response != null && response.contains(AppConstants.MATOMY_AD)) {
      Map<String, String> params = new HashMap<String, String>();
      params.put(AppConstants.RESPONSE, response);
      FlurryAgent.logEvent(FlurryData.MATOMY_AD_FULLSCREEN_FAILED, params);
    }
  }*/

}