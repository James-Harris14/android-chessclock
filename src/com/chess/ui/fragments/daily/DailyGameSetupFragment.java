package com.chess.ui.fragments.daily;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.chess.R;
import com.chess.backend.LoadHelper;
import com.chess.backend.entity.LoadItem;
import com.chess.backend.entity.new_api.DailySeekItem;
import com.chess.backend.tasks.RequestJsonTask;
import com.chess.ui.engine.configs.DailyGameConfig;
import com.chess.ui.fragments.CommonLogicFragment;
import com.chess.ui.fragments.friends.ChallengeFriendFragment;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 15.04.13
 * Time: 12:29
 */
public class DailyGameSetupFragment extends CommonLogicFragment {

	private static final String ERROR_TAG = "error popup";

	private DailyGameConfig.Builder gameConfigBuilder;
	private CreateChallengeUpdateListener createChallengeUpdateListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		gameConfigBuilder = new DailyGameConfig.Builder();
		createChallengeUpdateListener = new CreateChallengeUpdateListener();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_daily_game_setup_frame, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setTitle(R.string.daily);

		view.findViewById(R.id.dailyPlayBtn).setOnClickListener(this);
		view.findViewById(R.id.playFriendView).setOnClickListener(this);
		view.findViewById(R.id.dailyOptionsView).setOnClickListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		// TODO -> File | Settings | File Templates.
	}

	@Override
	public void onClick(View view) {
		super.onClick(view);

		int id = view.getId();
		if (id == R.id.dailyPlayBtn) {
			createDailyChallenge();

		} else if (id == R.id.playFriendView) {
			getActivityFace().changeRightFragment(new ChallengeFriendFragment());
			getActivityFace().toggleRightMenu();
		} else if (id == R.id.dailyOptionsView) {
			getActivityFace().changeRightFragment(new DailyGamesOptionsFragment());
			getActivityFace().toggleRightMenu();
		}
	}

	private void createDailyChallenge() {
		// create challenge using formed configuration
		DailyGameConfig dailyGameConfig = gameConfigBuilder.build();

		int color = dailyGameConfig.getUserColor();
		int days = dailyGameConfig.getDaysPerMove();
		int gameType = dailyGameConfig.getGameType();
		int isRated = dailyGameConfig.isRated() ? 1 : 0;
		String opponentName = dailyGameConfig.getOpponentName();

		LoadItem loadItem = LoadHelper.postGameSeek(getUserToken(), days, color, isRated, gameType, opponentName);
		new RequestJsonTask<DailySeekItem>(createChallengeUpdateListener).executeTask(loadItem);
	}

	private class CreateChallengeUpdateListener extends ChessUpdateListener<DailySeekItem> {

		public CreateChallengeUpdateListener() {
			super(DailySeekItem.class);
		}

		@Override
		public void updateData(DailySeekItem returnedObj) {
			showSinglePopupDialog(R.string.congratulations, R.string.daily_game_created);
		}

		@Override
		public void errorHandle(String resultMessage) {
			showPopupDialog(getString(R.string.error), resultMessage, ERROR_TAG);
		}
	}
}
