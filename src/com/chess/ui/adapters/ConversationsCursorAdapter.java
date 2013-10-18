package com.chess.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.chess.utilities.FontsHelper;
import com.chess.R;
import com.chess.RoboTextView;
import com.chess.backend.image_load.AvatarView;
import com.chess.backend.image_load.bitmapfun.SmartImageFetcher;
import com.chess.db.DbScheme;
import com.chess.ui.views.drawables.smart_button.ButtonDrawableBuilder;
import com.chess.utilities.AppUtils;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 01.08.13
 * Time: 17:07
 */
public class ConversationsCursorAdapter extends ItemsCursorAdapter {

	private final int paddingTop;
	private final int paddingSide;
	private final HashMap<String, SmartImageFetcher.Data> imageDataMap;

	private int imageSize;
	public ConversationsCursorAdapter(Context context, Cursor cursor, SmartImageFetcher imageFetcher) {
		super(context, cursor, imageFetcher);
		float density = resources.getDisplayMetrics().density;
		imageSize = (int) (40 * density);
		paddingTop = (int) (12 * density);
		paddingSide = (int) (12 * density);

		imageDataMap = new HashMap<String, SmartImageFetcher.Data>();
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = inflater.inflate(R.layout.new_conversation_list_item, parent, false);
		ViewHolder holder = new ViewHolder();
		holder.photoImg = (AvatarView) view.findViewById(R.id.photoImg);
		holder.authorTxt = (TextView) view.findViewById(R.id.authorTxt);
		holder.lastMessageTxt = (RoboTextView) view.findViewById(R.id.lastMessageTxt);
		holder.lastMessageDateTxt = (TextView) view.findViewById(R.id.lastMessageDateTxt);

		view.setTag(holder);

		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();

		boolean isOpponentUserOnline = getInt(cursor, DbScheme.V_OTHER_USER_IS_ONLINE) > 0;
		holder.photoImg.setOnline(isOpponentUserOnline);

		boolean haveNewMessages = getInt(cursor, DbScheme.V_NEW_MESSAGES_COUNT) > 0;
		if (haveNewMessages) {
			holder.lastMessageTxt.setFont(FontsHelper.BOLD_FONT);
			view.setBackgroundResource(R.drawable.transparent_list_item_selector);

		} else {
			holder.lastMessageTxt.setFont(FontsHelper.DEFAULT_FONT);
			ButtonDrawableBuilder.setBackgroundToView(view, R.style.ListItem_Header_2_Light);
		}
		view.setPadding(paddingSide, paddingTop, paddingSide, paddingTop);

		String otherUserAvatarUrl = getString(cursor, DbScheme.V_OTHER_USER_AVATAR_URL);
		if (!imageDataMap.containsKey(otherUserAvatarUrl)) {
			imageDataMap.put(otherUserAvatarUrl, new SmartImageFetcher.Data(otherUserAvatarUrl, imageSize));
		}

		imageFetcher.loadImage(imageDataMap.get(otherUserAvatarUrl), holder.photoImg.getImageView());

		holder.authorTxt.setText(getString(cursor, DbScheme.V_OTHER_USER_USERNAME));
		Spanned message = Html.fromHtml(getString(cursor, DbScheme.V_LAST_MESSAGE_CONTENT));
		holder.lastMessageTxt.setText(message);
		long timeAgo = getLong(cursor, DbScheme.V_LAST_MESSAGE_CREATED_AT);
		String lastDate = AppUtils.getMomentsAgoFromSeconds(timeAgo, context);
		holder.lastMessageDateTxt.setText(lastDate);
	}

	private static class ViewHolder {
		private AvatarView photoImg;
		private TextView authorTxt;
		private RoboTextView lastMessageTxt;
		private TextView lastMessageDateTxt;
	}
}
