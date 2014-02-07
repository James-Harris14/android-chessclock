package com.chess.ui.views.game_controls;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.chess.ui.interfaces.boards.BoardViewCompFace;

import static com.chess.ui.views.game_controls.ControlsBaseView.ButtonIds.*;

/**
 * GamePanelTestActivity class
 *
 * @author alien_roger
 * @created at: 06.03.12 7:39
 */
public class ControlsCompView extends ControlsBaseView {

	private BoardViewCompFace boardViewFace;

	public ControlsCompView(Context context) {
		super(context);
	}

	public ControlsCompView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void addButtons() {
		addControlButton(OPTIONS, styles[LEFT]);
		addControlButton(COMP, styles[MIDDLE]);
		addControlButton(HINT, styles[MIDDLE]);
		addControlButton(BACK, styles[MIDDLE]);
		addControlButton(FORWARD, styles[RIGHT]);
	}

	@Override
	public void onClick(View view) {  // TODO rework click handles
		super.onClick(view);
		if (blocked) {
			return;
		}

		if (view.getId() == getButtonId(HINT)) {
			boardViewFace.showHint();
		} else if (view.getId() == getButtonId(COMP)) {
			boardViewFace.computer();
		} else if (view.getId() == getButtonId(HELP)) {
			boardViewFace.switchAnalysis();
		}
	}

	public void setBoardViewFace(BoardViewCompFace boardViewFace) {
		super.setBoardViewFace(boardViewFace);
		this.boardViewFace = boardViewFace;
	}

	public void enableGameControls(boolean enable) {
		enableGameButton(OPTIONS, enable);
		enableGameButton(COMP, enable);
		enableGameButton(HINT, enable);
		enableGameButton(FORWARD, enable);
		enableGameButton(BACK, enable);
	}

	public void enableHintButton(boolean enable) {
		enableGameButton(COMP, enable);
		enableGameButton(HINT, enable);
	}

}