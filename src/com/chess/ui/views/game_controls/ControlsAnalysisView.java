package com.chess.ui.views.game_controls;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.chess.R;
import com.chess.ui.interfaces.boards.BoardViewAnalysisFace;

import static com.chess.ui.views.game_controls.ControlsBaseView.ButtonIds.*;

/**
 * GamePanelTestActivity class
 *
 * @author alien_roger
 * @created at: 06.03.12 7:39
 */
public class ControlsAnalysisView extends ControlsBaseView {

	private BoardViewAnalysisFace boardViewFace;

	public ControlsAnalysisView(Context context) {
		super(context);
	}

	public ControlsAnalysisView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	void init() {
		super.init();

		addControlButton(EXIT, R.style.Rect_Bottom_Right);
		addControlButton(SEARCH, R.style.Rect_Bottom_Middle);
		addControlButton(RESTART, R.style.Rect_Bottom_Left);
		addControlButton(BACK, R.style.Rect_Bottom_Middle);
		addControlButton(FORWARD, R.style.Rect_Bottom_Middle);

		addView(controlsLayout);
	}

	@Override
	public void enableForwardBtn(boolean enable) {
//		enableGameButton(FORWARD, enable);
	}

	@Override
	public void enableBackBtn(boolean enable) {
//		enableGameButton(BACK, enable);
	}

	@Override
	public void onClick(View view) {  // TODO rework click handles
		if (blocked)
			return;

		if (view.getId() == getButtonId(EXIT)) {
			boardViewFace.closeBoard();
		} else if (view.getId() == getButtonId(SEARCH)) {
			// TODO add search ability
		} else if (view.getId() == getButtonId(RESTART)) {
			boardViewFace.restart();
		} else if (view.getId() == getButtonId(BACK)) {
			boardViewFace.moveBack();
		} else if (view.getId() == getButtonId(FORWARD)) {
			boardViewFace.moveForward();
		}
	}

	public void setBoardViewFace(BoardViewAnalysisFace boardViewFace) {
		this.boardViewFace = boardViewFace;
	}

	public void enableControlButtons(boolean enable) {
		enableGameButton(FORWARD, enable);
		enableGameButton(BACK, enable);
	}

	public void enableGameControls(boolean enable) {
		enableGameButton(SEARCH, enable);
		enableGameButton(RESTART, enable);
		enableGameButton(BACK, enable);
		enableGameButton(FORWARD, enable);
	}
}