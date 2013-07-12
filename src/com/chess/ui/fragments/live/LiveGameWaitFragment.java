package com.chess.ui.fragments.live;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.chess.R;
import com.chess.backend.LiveChessService;
import com.chess.backend.interfaces.ActionBarUpdateListener;
import com.chess.lcc.android.DataNotValidException;
import com.chess.lcc.android.interfaces.LccEventListener;
import com.chess.live.client.Game;
import com.chess.model.GameLiveItem;
import com.chess.ui.engine.configs.LiveGameConfig;
import com.chess.ui.fragments.LiveBaseFragment;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 30.04.13
 * Time: 9:30
 */
public class LiveGameWaitFragment extends LiveBaseFragment implements LccEventListener {

	private static final String CONFIG = "config";
	private View loadingView;
	private LiveGameConfig liveGameConfig;
	private GameTaskListener gameTaskListener;

	public LiveGameWaitFragment() {
		Bundle bundle = new Bundle();
		bundle.putParcelable(CONFIG, new LiveGameConfig.Builder().build());
		setArguments(bundle);
	}

	public static LiveGameWaitFragment createInstance(LiveGameConfig config) {
		LiveGameWaitFragment fragment = new LiveGameWaitFragment();
		Bundle bundle = new Bundle();
		bundle.putParcelable(CONFIG, config);
		fragment.setArguments(bundle);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		gameTaskListener = new GameTaskListener();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_live_game_wait_frame, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setTitle(R.string.live_chess);

		loadingView = view.findViewById(R.id.loadingView);
		view.findViewById(R.id.cancelLiveBtn).setOnClickListener(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (getArguments() != null) {
			liveGameConfig = getArguments().getParcelable(CONFIG);
		} else {
			liveGameConfig = savedInstanceState.getParcelable(CONFIG);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		getAppData().setLiveChessMode(true);
		liveBaseActivity.connectLcc();
		loadingView.setVisibility(View.VISIBLE);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("smth", "value");
		super.onSaveInstanceState(outState);
		outState.putParcelable(CONFIG, liveGameConfig);
	}

	@Override
	public void onLiveServiceConnected() {
		super.onLiveServiceConnected();
		LiveChessService liveService;
		try {
			liveService = getLiveService();
		} catch (DataNotValidException e) {
			logTest(e.getMessage());
			backToHomeFragment();
			return;
		}
		liveService.setLccEventListener(this);
		liveService.setGameTaskListener(gameTaskListener);
	}

	@Override
	public void onClick(View view) {
		super.onClick(view);

		if (view.getId() == R.id.cancelLiveBtn) {
			logoutFromLive();

			getActivityFace().showPreviousFragment();
		}
	}

	@Override
	public void setWhitePlayerTimer(String timer) {
	}

	@Override
	public void setBlackPlayerTimer(String timer) {
	}

	@Override
	public void onGameRefresh(GameLiveItem gameItem) {
	}

	@Override
	public void onDrawOffered(String drawOfferUsername) {
	}

	@Override
	public void onGameEnd(String gameEndMessage) {
	}

	@Override
	public void onInform(String title, String message) {
	}

//	@Override
//	public void onGameRecreate() {
//	}

	@Override
	public void startGameFromService() {
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
						getActivityFace().showPreviousFragment();   // TODO handle correctly
						return;
					}
					loadingView.setVisibility(View.GONE);
					logTest("challenge created, ready to start");

					Long gameId = liveService.getCurrentGameId();
					logTest("gameId = " + gameId);
					getActivityFace().switchFragment(GameLiveFragment.createInstance(gameId));
				}
			});
		}
	}

	@Override
	public void createSeek() {
		if (liveGameConfig != null) {
			LiveChessService liveService;
			try {
				liveService = getLiveService();
			} catch (DataNotValidException e) {
				logTest(e.getMessage());
				getActivityFace().showPreviousFragment();
				return;
			}
			liveService.createChallenge(liveGameConfig);
		}
	}

	private class GameTaskListener extends ActionBarUpdateListener<Game> {
		public GameTaskListener() {
			super(getInstance());
		}
	}
}