package com.chess.ui.fragments.messages;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.chess.R;
import com.chess.model.SelectionItem;
import com.chess.ui.adapters.ItemsAdapter;
import com.chess.ui.fragments.BasePopupsFragment;
import com.chess.ui.fragments.CommonLogicFragment;
import com.chess.ui.fragments.articles.ArticleCategoriesFragmentTablet;
import com.chess.ui.fragments.friends.FriendsFragmentTablet;
import com.chess.ui.interfaces.FragmentParentFace;
import com.chess.ui.views.drawables.smart_button.ButtonDrawableBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 16.11.13
 * Time: 17:38
 */
public class MessagesFragmentTablet extends CommonLogicFragment implements FragmentParentFace, AdapterView.OnItemClickListener {

	private boolean noCategoriesFragmentsAdded;
	private List<SelectionItem> menuItems;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		init();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_common_tablet_content_frame, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		ListView listView = (ListView) view.findViewById(R.id.listView);
		listView.setAdapter(new OptionsAdapter(getActivity(), menuItems));
		listView.setOnItemClickListener(this);
	}

	protected void init() {

		menuItems = new ArrayList<SelectionItem>();
		{ // inbox
			SelectionItem selectionItem = new SelectionItem(null, getString(R.string.inbox));
			selectionItem.setCode(getString(R.string.ic_inbox));
			menuItems.add(selectionItem);
		}
		{ // new
			SelectionItem selectionItem = new SelectionItem(null, getString(R.string.new_));
			selectionItem.setCode(getString(R.string.ic_edit));
			menuItems.add(selectionItem);
		}
		changeInternalFragment(new MessagesInboxFragmentTablet(this));

		noCategoriesFragmentsAdded = true;
	}

	@Override
	public void changeFragment(BasePopupsFragment fragment) {
		openInternalFragment(fragment);
	}

	private void changeInternalFragment(Fragment fragment) {
		FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
		transaction.replace(R.id.innerFragmentContainer, fragment).commitAllowingStateLoss();
	}

	private void openInternalFragment(Fragment fragment) {
		FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
		transaction.replace(R.id.innerFragmentContainer, fragment, fragment.getClass().getSimpleName());
		transaction.addToBackStack(fragment.getClass().getSimpleName());
		transaction.commit();
	}

	@Override
	public boolean showPreviousFragment() {
		if (getActivity() == null) {
			return false;
		}
		int entryCount = getChildFragmentManager().getBackStackEntryCount();
		if (entryCount > 0) {
			int last = entryCount - 1;
			FragmentManager.BackStackEntry stackEntry = getChildFragmentManager().getBackStackEntryAt(last);
			if (stackEntry != null && stackEntry.getName().equals(MessagesInboxFragmentTablet.class.getSimpleName())) {
				noCategoriesFragmentsAdded = true;    // TODO adjust
			}

			return getChildFragmentManager().popBackStackImmediate();
		} else {
			return super.showPreviousFragment();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (position == 0) {
			Fragment fragmentByTag = getChildFragmentManager().findFragmentByTag(MessagesInboxFragmentTablet.class.getSimpleName());
			if (fragmentByTag == null) {
				fragmentByTag = new MessagesInboxFragmentTablet(this);
			}
			changeInternalFragment(fragmentByTag);
		} else {
			Fragment fragmentByTag = getChildFragmentManager().findFragmentByTag(NewMessageFragment.class.getSimpleName());
			if (fragmentByTag == null) {
				fragmentByTag = new NewMessageFragment();
			}
			changeInternalFragment(fragmentByTag);
		}
	}

	private class OptionsAdapter extends ItemsAdapter<SelectionItem> {

		private final int sidePadding;
		private final int whiteColor;

		public OptionsAdapter(Context context, List<SelectionItem> itemList) {
			super(context, itemList);
			sidePadding = (int) (8 * density);
			whiteColor = resources.getColor(R.color.semitransparent_white_75);
		}

		@Override
		protected View createView(ViewGroup parent) {
			View view = inflater.inflate(R.layout.new_dark_spinner_item, parent, false);

			ButtonDrawableBuilder.setBackgroundToView(view, R.style.ListItem_Header_Dark);
			view.setPadding(sidePadding, 0, sidePadding, 0);

			ViewHolder holder = new ViewHolder();
			holder.nameTxt = (TextView) view.findViewById(R.id.categoryNameTxt);
			holder.iconTxt = (TextView) view.findViewById(R.id.iconTxt);
			holder.spinnerIcon = (TextView) view.findViewById(R.id.spinnerIcon);
			holder.spinnerIcon.setVisibility(View.GONE);

			holder.nameTxt.setPadding(sidePadding, 0, 0, 0);
			holder.nameTxt.setTextColor(whiteColor);
			holder.iconTxt.setVisibility(View.VISIBLE);

			view.setTag(holder);
			return view;
		}

		@Override
		protected void bindView(SelectionItem item, int pos, View convertView) {
			ViewHolder holder = (ViewHolder) convertView.getTag();

			holder.nameTxt.setText(item.getText());
			holder.iconTxt.setText(item.getCode());
		}

		private class ViewHolder {
			TextView iconTxt;
			TextView nameTxt;
			TextView spinnerIcon;
		}
	}
}