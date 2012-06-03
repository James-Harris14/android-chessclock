package com.chess.ui.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import com.chess.R;
import com.chess.backend.statics.AppConstants;
import com.chess.backend.statics.AppData;
import com.chess.backend.statics.IntentConstants;
import com.chess.backend.statics.StaticData;
import com.chess.lcc.android.LccHolder;
import com.chess.model.GameListItem;
import com.chess.utilities.MopubHelper;
import com.mopub.mobileads.MoPubView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * LiveScreenActivity class
 *
 * @author alien_roger
 * @created at: 08.02.12 7:17
 */
public class LiveScreenActivity extends LiveBaseActivity implements View.OnClickListener {

	private TextView startNewGameTitle;

	private Button currentGame;
	private Button start;
	private GridView gridview;

	private int temp_pos = -1;
	public static int ONLINE_CALLBACK_CODE = 32;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.live_screen);

		widgetsInit();
		init();

		gridview.setAdapter(new NewGamesButtonsAdapter());
	}

	@Override
    protected void widgetsInit(){
		Button upgradeBtn = (Button) findViewById(R.id.upgradeBtn);
		upgradeBtn.setOnClickListener(this);

		if (MopubHelper.isShowAds(mainApp)) {
			moPubView = (MoPubView) findViewById(R.id.mopub_adview);
			MopubHelper.showBannerAd(upgradeBtn, moPubView, mainApp);
		}

		startNewGameTitle = (TextView) findViewById(R.id.startNewGameTitle);

		start = (Button) findViewById(R.id.start);
		start.setOnClickListener(this);

		gridview = (GridView) findViewById(R.id.gridview);

		currentGame = (Button) findViewById(R.id.currentGame);
		currentGame.setOnClickListener(this);

		if (lccHolder.isConnected()) {
			start.setVisibility(View.VISIBLE);
			gridview.setVisibility(View.VISIBLE);
			startNewGameTitle.setVisibility(View.VISIBLE);
		}

	}

	private void init() {
		mainApp.setLiveChess(true);
	}

	@Override
	protected void onResume() {
		if (!lccHolder.isConnected()) {
			start.setVisibility(View.GONE);
			gridview.setVisibility(View.GONE);
			startNewGameTitle.setVisibility(View.GONE);
		}

		registerReceiver(lccLoggingInInfoReceiver, new IntentFilter(IntentConstants.FILTER_LOGINING_INFO));

		super.onResume();
		if (mainApp.isLiveChess() && lccHolder.getCurrentGameId() != null &&
				lccHolder.getGame(lccHolder.getCurrentGameId()) != null) {
			currentGame.setVisibility(View.VISIBLE);
		} else {
			currentGame.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onPause() {
		unregisterReceiver(this.lccLoggingInInfoReceiver);
		super.onPause();
	}

	private class NewGamesButtonsAdapter extends BaseAdapter {

		private LayoutInflater inflater;

		private NewGamesButtonsAdapter() {
			this.inflater = LayoutInflater.from(getContext());
		}

		@Override
		public int getCount() {
			return StartNewGameButtonsEnum.values().length;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Button button;
			if (convertView == null) {
				button = (Button) inflater.inflate(R.layout.default_button_grey, null, false);
			} else {
				button = (Button) convertView;
			}
			StartNewGameButtonsEnum.values();
			final StartNewGameButtonsEnum startNewGameButton = StartNewGameButtonsEnum.values()[position];
			button.setText(startNewGameButton.getText());
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					preferencesEditor.putString(AppConstants.CHALLENGE_INITIAL_TIME, StaticData.SYMBOL_EMPTY + startNewGameButton.getMin());
					preferencesEditor.putString(AppConstants.CHALLENGE_BONUS_TIME, StaticData.SYMBOL_EMPTY + startNewGameButton.getSec());
					preferencesEditor.commit();
					startActivity(new Intent(getContext(), LiveCreateChallengeActivity.class));
				}
			});
			return button;
		}
	}

	@Override
	public void update(int code) {
		if (code == INIT_ACTIVITY) {
			if (appService != null) {
				update(ONLINE_CALLBACK_CODE);
			}
		} else if (code == ONLINE_CALLBACK_CODE) {

		} else if (code == 1) {
			onPause();
			onResume();
		} else if (code == 2) {
			onPause();
			onResume();
			showToast(R.string.challengeaccepted);
		} else if (code == 3) {
			onPause();
			onResume();
			showToast(R.string.challengedeclined);
		} else if (code == 4) {
			onPause();
			onResume();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case 0: {
				if (temp_pos > -1) {
					return new AlertDialog.Builder(this)
							.setTitle(getString(R.string.accept_draw_q))
							.setPositiveButton(getString(R.string.accept), acceptDrawDialogListener)
							.setNeutralButton(getString(R.string.decline), acceptDrawDialogListener)
							.setNegativeButton(getString(R.string.game), acceptDrawDialogListener).create();
				}
			}
			default:
				break;
		}
		return super.onCreateDialog(id);
	}

	private  DialogInterface.OnClickListener acceptDrawDialogListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int whichButton) {
			GameListItem gameListElement = mainApp.getGameListItems().get(temp_pos);

			switch (whichButton) {
				case DialogInterface.BUTTON_POSITIVE: {
					if (appService != null) {
						appService.RunSingleTask(4,
								"http://www." + LccHolder.HOST + AppConstants.API_SUBMIT_ECHESS_ACTION_ID
										+ preferences.getString(AppConstants.USER_TOKEN, StaticData.SYMBOL_EMPTY)
										+ AppConstants.CHESSID_PARAMETER + gameListElement.getGameId()
										+ "&command=ACCEPTDRAW&timestamp="
										+ gameListElement.values.get(GameListItem.TIMESTAMP),
								null);
					}
				}
				break;
				case DialogInterface.BUTTON_NEUTRAL: {
					if (appService != null) {
						appService.RunSingleTask(4,
								"http://www." + LccHolder.HOST
										+ AppConstants.API_SUBMIT_ECHESS_ACTION_ID
										+ preferences.getString(AppConstants.USER_TOKEN, StaticData.SYMBOL_EMPTY)
										+ AppConstants.CHESSID_PARAMETER + gameListElement.getGameId()
										+ "&command=DECLINEDRAW&timestamp="
										+ gameListElement.values.get(GameListItem.TIMESTAMP),
								null);
					}
				}
				break;
				case DialogInterface.BUTTON_NEGATIVE: {
					startActivity(new Intent(getContext(), GameLiveScreenActivity.class).
							putExtra(AppConstants.GAME_MODE, AppConstants.GAME_MODE_LIVE_OR_ECHESS).
							putExtra(GameListItem.GAME_ID, gameListElement.getGameId()));

				}
				break;
				default:
					break;
			}
		}
	};

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.upgradeBtn) {
			startActivity(AppData.getMembershipAndroidIntent(this));
		} else if (view.getId() == R.id.tournaments) {
			String GOTO = "http://www." + LccHolder.HOST + AppConstants.TOURNAMENTS;
			try {
				GOTO = URLEncoder.encode(GOTO, AppConstants.UTF_8);
			} catch (UnsupportedEncodingException ignored) {
			}

			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www."
					+ LccHolder.HOST + AppConstants.LOGIN_HTML_ALS
					+ preferences.getString(AppConstants.USER_TOKEN, StaticData.SYMBOL_EMPTY)
					+ "&goto=" + GOTO)));
		} else if (view.getId() == R.id.stats) {
			String GOTO = "http://www." + LccHolder.HOST + AppConstants.ECHESS_MOBILE_STATS
					+ AppData.getUserName(getContext());
			try {
				GOTO = URLEncoder.encode(GOTO, AppConstants.UTF_8);
			} catch (UnsupportedEncodingException ignored) {
			}

			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www." + LccHolder.HOST
					+ AppConstants.LOGIN_HTML_ALS + preferences.getString(AppConstants.USER_TOKEN, StaticData.SYMBOL_EMPTY)
					+ "&goto=" + GOTO)));
		} else if (view.getId() == R.id.currentGame) {

			if (lccHolder.getCurrentGameId() != null && lccHolder.getGame(lccHolder.getCurrentGameId()) != null) {
				lccHolder.processFullGame(lccHolder.getGame(lccHolder.getCurrentGameId()));
			}

		} else if (view.getId() == R.id.start) {
			startActivity(new Intent(this, LiveNewGameActivity.class));
		}
	}

	private enum StartNewGameButtonsEnum {
		BUTTON_10_0(10, 0, "10 min"),
		BUTTON_5_2(5, 2, "5 | 2"),
		BUTTON_15_10(15, 10, "15 | 10"),
		BUTTON_30_0(30, 0, "30 min"),
		BUTTON_5_0(5, 0, "5 min"),
		BUTTON_3_0(3, 0, "3 min"),
		BUTTON_2_1(2, 1, "2 | 1"),
		BUTTON_1_0(1, 0, "1 min");

		private int min;
		private int sec;
		private String text;

		private StartNewGameButtonsEnum(int min, int sec, String text) {
			this.min = min;
			this.sec = sec;
			this.text = text;
		}

		public int getMin() {
			return min;
		}

		public int getSec() {
			return sec;
		}

		public String getText() {
			return text;
		}
	}

	private BroadcastReceiver lccLoggingInInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, final Intent intent) {
			if (!intent.getExtras().getBoolean(AppConstants.ENABLE_LIVE_CONNECTING_INDICATOR)) {
				start.setVisibility(View.VISIBLE);
				if (gridview != null) {
					gridview.setVisibility(View.VISIBLE);
				}
				startNewGameTitle.setVisibility(View.VISIBLE);
			}
		}
	};

}