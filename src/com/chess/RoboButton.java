package com.chess;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.Button;
import com.chess.ui.views.drawables.smart_button.ButtonDrawable;
import com.chess.ui.views.drawables.smart_button.ButtonDrawableBuilder;
import com.chess.ui.views.drawables.smart_button.RectButtonDrawable;
import com.chess.utilities.AppUtils;

import java.io.Serializable;

public class RoboButton extends Button implements Serializable {

	private static final long serialVersionUID = -7816685707888388856L;

	private String ttfName = "Bold";

	public RoboButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setupFont(context, attrs);
	}

	public RoboButton(Context context) {
		super(context);
	}

	public RoboButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupFont(context, attrs);
	}

	private void setupFont(Context context, AttributeSet attrs) {
		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RoboTextView);
		if (array == null) {
			return;
		}
		try {
			if (array.hasValue(R.styleable.RoboTextView_ttf)) {
				ttfName = array.getString(R.styleable.RoboTextView_ttf);
			}
		} finally {
			array.recycle();
		}

		init(context, attrs);
	}

    private void init(Context context, AttributeSet attrs) {
		if (!isInEditMode()) {
			Typeface font = Typeface.createFromAsset(context.getAssets(), RoboTextView.MAIN_PATH + ttfName + ".ttf");
			setTypeface(font);
		}

		if (attrs != null) {
			TypedArray array =context.obtainStyledAttributes(attrs, R.styleable.RoboButton);
			if (array == null) {
				return;
			}
			boolean isRect = false;
			try {
				if (!array.hasValue(R.styleable.RoboButton_btn_is_solid)) {
					return;
				}
				isRect = array.getBoolean(R.styleable.RoboButton_btn_is_rect, false);
			} finally {
				array.recycle();
			}

			if (isRect) {
				RectButtonDrawable background = new RectButtonDrawable(getContext(), attrs);
				if (AppUtils.HONEYCOMB_PLUS_API) {
					setBackground(background);
				} else {
					setBackgroundDrawable(background);
				}
			} else {
				ButtonDrawable background = new ButtonDrawable(getContext(), attrs);
				if (AppUtils.HONEYCOMB_PLUS_API) {
					setBackground(background);
				} else {
					setBackgroundDrawable(background);
				}
			}
		}

    }

	public void setFont(String font) {
		ttfName = font;
		init(getContext(), null);
	}

	public void setDrawableStyle(int styleId) {
		ButtonDrawable buttonDrawable = ButtonDrawableBuilder.createDrawable(getContext(), styleId);
		if (AppUtils.HONEYCOMB_PLUS_API) {
			setBackground(buttonDrawable);
		} else {
			setBackgroundDrawable(buttonDrawable);
		}
	}
}
