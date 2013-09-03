package com.chess.ui.views.game_controls;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.chess.R;
import com.chess.RoboButton;
import com.chess.ui.interfaces.boards.BoardViewTacticsFace;

import static com.chess.ui.views.game_controls.ControlsBaseView.ButtonIds.*;

/**
 * GamePanelTestActivity class
 *
 * @author alien_roger
 * @created at: 06.03.12 7:39
 */
public class ControlsTacticsView extends ControlsBaseView {

	private BoardViewTacticsFace boardViewFace;
	private State state;

	public ControlsTacticsView(Context context) {
		super(context);
	}

	public ControlsTacticsView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	void init() {
		super.init();

		removeAllViews();

		addControlButton(OPTIONS, R.style.Rect_Bottom_Left);
		addControlButton(ANALYSIS, R.style.Rect_Bottom_Middle);
		addControlButton(HINT, R.style.Rect_Bottom_Middle);
		addControlButton(HELP, R.style.Rect_Bottom_Middle);
		addControlButton(CLOSE, R.style.Rect_Bottom_Middle);
		addControlButton(RESTORE, R.style.Rect_Bottom_Middle);
		addControlButton(SEARCH, R.style.Rect_Bottom_Middle);
		addControlButton(FLIP, R.style.Rect_Bottom_Middle);
		addControlButton(BACK, R.style.Rect_Bottom_Middle);
		addControlButton(FORWARD, R.style.Rect_Bottom_Right);

		addNextButton(R.style.Rect_Bottom_Right_Green, NEXT);
		addNextButton(R.style.Rect_Bottom_Right_Orange, SKIP);
		addWrongButton();

		addView(controlsLayout);

		showDefault();
	}

	protected void addNextButton(int styleId, ButtonIds id) {
		RoboButton button = getDefaultButton();
		button.setText(R.string.ic_arrow_right);
		button.setDrawableStyle(styleId);
		button.setId(getButtonId(id));
		button.setVisibility(GONE);
		LayoutParams params = new LayoutParams(0, controlButtonHeight);

		params.weight = 2;

		controlsLayout.addView(button, params);
	}

	protected void addWrongButton() {
		RoboButton button = getDefaultButton();
		button.setText(R.string.ic_restore);
		button.setDrawableStyle(R.style.Rect_Bottom_Right_Red);
		button.setId(getButtonId(RESTART));
		button.setVisibility(GONE);
		LayoutParams params = new LayoutParams(0, controlButtonHeight);

		params.weight = 2;

		controlsLayout.addView(button, params);
	}

	public void setBoardViewFace(BoardViewTacticsFace boardViewFace) {
		this.boardViewFace = boardViewFace;
	}

	@Override
	public void onClick(View view) {  // TODO rework click handles
		if (blocked)
			return;

		if (view.getId() == getButtonId(OPTIONS)) {
			boardViewFace.showOptions(view);
		} else if (view.getId() == getButtonId(RESTART) || view.getId() == getButtonId(RESTORE)) {
			boardViewFace.restart();
		} else if (view.getId() == getButtonId(HINT)) {
			boardViewFace.showHint();
		} else if (view.getId() == getButtonId(SEARCH)) {
			boardViewFace.showExplorer();
		} else if (view.getId() == getButtonId(FLIP)) {
			boardViewFace.flipBoard();
		} else if (view.getId() == getButtonId(ANALYSIS) || view.getId() == getButtonId(CLOSE)) {
			boardViewFace.switchAnalysis();
		} else if (view.getId() == getButtonId(HELP)) {
			boardViewFace.showHint();
		} else if (view.getId() == getButtonId(BACK)) {
			boardViewFace.moveBack();
		} else if (view.getId() == getButtonId(FORWARD)) {
			boardViewFace.moveForward();
		} else if (view.getId() == getButtonId(NEXT) || view.getId() == getButtonId(SKIP)) {
			boardViewFace.newGame();
		}
	}

	public void showWrong() {
		state = State.WRONG;

		showGameButton(OPTIONS, true);
		showGameButton(HINT, true);
		showGameButton(HELP, false);
		showGameButton(ANALYSIS, false);
		showGameButton(RESTORE, false);
		showGameButton(CLOSE, false);
		showGameButton(SEARCH, false);
		showGameButton(FLIP, false);
		showGameButton(BACK, false);
		showGameButton(FORWARD, false);
		showGameButton(SKIP, false);
		showGameButton(RESTART, true);
	}

	public void showCorrect() {
		state = State.CORRECT;

		showGameButton(OPTIONS, true);
		showGameButton(HINT, false);
		showGameButton(HELP, false);
		showGameButton(BACK, false);
		showGameButton(FORWARD, false);
		showGameButton(ANALYSIS, true);
		showGameButton(NEXT, true);
		showGameButton(SKIP, false);
		showGameButton(RESTORE, false);
		showGameButton(CLOSE, false);
		showGameButton(SEARCH, false);
		showGameButton(FLIP, false);
		showGameButton(BACK, false);
		showGameButton(FORWARD, false);
	}

	public void showDefault() {
		state = State.DEFAULT;

		showGameButton(OPTIONS, true);
		showGameButton(RESTART, false);
		showGameButton(FLIP, false);
		showGameButton(ANALYSIS, false);
		showGameButton(HELP, true);
		showGameButton(HINT, false);
		showGameButton(NEXT, false);
		showGameButton(SKIP, false);
		showGameButton(RESTORE, false);
		showGameButton(CLOSE, false);
		showGameButton(SEARCH, false);
		showGameButton(FLIP, false);
		showGameButton(BACK, false);
		showGameButton(FORWARD, false);
	}

	public void showAfterRetry() {
		state = State.AFTER_RETRY;

		showGameButton(OPTIONS, true);
		showGameButton(RESTART, false);
		showGameButton(FLIP, false);
		showGameButton(ANALYSIS, true);
		showGameButton(HELP, false);
		showGameButton(HINT, false);
		showGameButton(NEXT, false);
		showGameButton(SKIP, true);
		showGameButton(RESTORE, false);
		showGameButton(CLOSE, false);
		showGameButton(SEARCH, false);
		showGameButton(FLIP, false);
		showGameButton(BACK, false);
		showGameButton(FORWARD, false);
	}

	public void showAnalysis() {
		state = State.ANALYSIS;

		showGameButton(OPTIONS, false);
		showGameButton(RESTART, false);
		showGameButton(ANALYSIS, false);
		showGameButton(HELP, false);
		showGameButton(HINT, false);
		showGameButton(NEXT, false);
		showGameButton(SKIP, false);
		showGameButton(CLOSE, true);
		showGameButton(SEARCH, true);
		showGameButton(RESTORE, true);
		showGameButton(FLIP, true);
		showGameButton(BACK, true);
		showGameButton(FORWARD, true);
	}

	public void enableGameControls(boolean enable) {
		enableGameButton(OPTIONS, enable);
		enableGameButton(HELP, enable);
	}

	@Override
	public void enableForwardBtn(boolean enable) {
//		enableGameButton(FORWARD, enable);
	}

	@Override
	public void enableBackBtn(boolean enable) {
//		enableGameButton(BACK, enable);
	}

	public State getState() {
		return state;
	}

	public enum State {
		DEFAULT,
		WRONG,
		CORRECT,
		AFTER_RETRY,
		ANALYSIS
	}
}