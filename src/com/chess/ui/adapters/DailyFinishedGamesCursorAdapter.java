package com.chess.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.chess.R;
import com.chess.backend.RestHelper;
import com.chess.backend.image_load.AvatarView;
import com.chess.backend.statics.Symbol;
import com.chess.db.DbScheme;
import com.chess.model.BaseGameItem;
import com.chess.utilities.AppUtils;

public class DailyFinishedGamesCursorAdapter extends ItemsCursorAdapter {

	protected static final String CHESS_960 = " (960)";
	private final int imageSize;
	private final String drawStr;
	private final String lossStr;
	private final String winStr;
	private final int colorOrange;
	private final int colorGreen;
	private final int colorGrey;

	public DailyFinishedGamesCursorAdapter(Context context, Cursor cursor) {
		super(context, cursor);
		imageSize = (int) (resources.getDimension(R.dimen.list_item_image_size_big) / resources.getDisplayMetrics().density);

		lossStr = context.getString(R.string.loss);
		winStr = context.getString(R.string.won);
		drawStr = context.getString(R.string.draw);

		colorOrange = resources.getColor(R.color.orange_button_flat);
		colorGreen = resources.getColor(R.color.new_dark_green);
		colorGrey = resources.getColor(R.color.stats_label_grey);

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = inflater.inflate(R.layout.new_daily_finished_games_item, parent, false);
		ViewHolder holder = new ViewHolder();
		holder.playerImg = (AvatarView) view.findViewById(R.id.playerImg);
		holder.playerTxt = (TextView) view.findViewById(R.id.playerNameTxt);
		holder.premiumImg = (ImageView) view.findViewById(R.id.premiumImg);
		holder.ratingTxt = (TextView) view.findViewById(R.id.ratingTxt);
		holder.gameResultTxt = (TextView) view.findViewById(R.id.gameResultTxt);
		holder.gameTypeTxt = (TextView) view.findViewById(R.id.gameTypeTxt);

		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View convertView, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) convertView.getTag();

		// get player side, and choose opponent
		String avatarUrl;
		String opponentName;
		String opponentRating;
		int premiumStatus;
		if (getInt(cursor, DbScheme.V_I_PLAY_AS) == RestHelper.P_BLACK) {
			avatarUrl = getString(cursor, DbScheme.V_WHITE_AVATAR);
			opponentName = getString(cursor, DbScheme.V_WHITE_USERNAME);
			opponentRating = getString(cursor, DbScheme.V_WHITE_RATING);
			premiumStatus = getInt(cursor, DbScheme.V_WHITE_PREMIUM_STATUS);
		} else {
			avatarUrl = getString(cursor, DbScheme.V_BLACK_AVATAR);
			opponentName = getString(cursor, DbScheme.V_BLACK_USERNAME);
			opponentRating = getString(cursor, DbScheme.V_BLACK_RATING);
			premiumStatus = getInt(cursor, DbScheme.V_BLACK_PREMIUM_STATUS);
		}

		holder.premiumImg.setImageResource(AppUtils.getPremiumIcon(premiumStatus));
		holder.playerTxt.setText(opponentName);
		holder.ratingTxt.setText(Symbol.wrapInPars(opponentRating));
		imageLoader.download(avatarUrl, holder.playerImg, imageSize);

		boolean isOpponentOnline = getInt(cursor, DbScheme.V_IS_OPPONENT_ONLINE) > 0;
		holder.playerImg.setOnline(isOpponentOnline);

		if(getInt(cursor, DbScheme.V_GAME_TYPE) == RestHelper.V_GAME_CHESS) {
			holder.gameTypeTxt.setText(R.string.ic_daily_game);
		} else {
			holder.gameTypeTxt.setText(R.string.ic_daily960_game);
		}

		// Loss orange
		String result = lossStr;
		holder.gameResultTxt.setTextColor(colorOrange);
		if (getInt(cursor, DbScheme.V_GAME_SCORE) == BaseGameItem.GAME_WON) {
			// Win Green
			result = winStr;
			holder.gameResultTxt.setTextColor(colorGreen);
		} else if (getInt(cursor, DbScheme.V_GAME_SCORE) == BaseGameItem.GAME_DRAW) {
			// Draw Grey
			result = drawStr;
			holder.gameResultTxt.setTextColor(colorGrey);
		}
		holder.gameResultTxt.setText(result);
	}

	protected class ViewHolder {
		public AvatarView playerImg;
		public TextView playerTxt;
		public ImageView premiumImg;
		public TextView ratingTxt;
		public TextView gameResultTxt;
		public TextView gameTypeTxt;
	}
}
