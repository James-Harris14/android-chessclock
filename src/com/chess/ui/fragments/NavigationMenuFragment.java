package com.chess.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.chess.R;
import com.chess.ui.adapters.ItemsAdapter;
import com.slidingmenu.lib.SlidingMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 02.01.13
 * Time: 11:04
 */
public class NavigationMenuFragment extends CommonLogicFragment implements AdapterView.OnItemClickListener {

	private static final int UPGRADE_POS = 0;

	private ListView listView;
	private List<NavigationMenuItem> menuItems;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		menuItems = new ArrayList<NavigationMenuItem>();
		menuItems.add(new NavigationMenuItem(getString(R.string.nav_upgrade), R.drawable.ic_nav_upgrade));
		menuItems.add(new NavigationMenuItem(getString(R.string.nav_home), R.drawable.ic_nav_home));
		menuItems.add(new NavigationMenuItem(getString(R.string.nav_play), R.drawable.ic_nav_play));
		menuItems.add(new NavigationMenuItem(getString(R.string.nav_tactics), R.drawable.ic_nav_tactics));
		menuItems.add(new NavigationMenuItem(getString(R.string.nav_videos), R.drawable.ic_nav_videos));
		menuItems.add(new NavigationMenuItem(getString(R.string.nav_articles), R.drawable.ic_nav_articles));
		menuItems.add(new NavigationMenuItem(getString(R.string.nav_stats), R.drawable.ic_nav_stats));
		menuItems.add(new NavigationMenuItem(getString(R.string.nav_friends), R.drawable.ic_nav_friends));
		menuItems.add(new NavigationMenuItem(getString(R.string.nav_messages), R.drawable.ic_nav_messages));
		menuItems.add(new NavigationMenuItem(getString(R.string.nav_settings), R.drawable.ic_nav_settings));

	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_navigation_menu_frame, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		listView = (ListView) view.findViewById(R.id.listView);
		listView.setOnItemClickListener(this);
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		NewNavigationMenuAdapter adapter = new NewNavigationMenuAdapter(getActivity(), menuItems);

		listView.setAdapter(adapter);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		for (NavigationMenuItem menuItem : menuItems) {
			menuItem.selected = false;
		}

		menuItems.get(position).selected = true;
		NavigationMenuItem menuItem = (NavigationMenuItem) listView.getItemAtPosition(position);
		menuItem.selected = true;
		((BaseAdapter)parent.getAdapter()).notifyDataSetChanged();

		switch (menuItem.iconRes) {
			case R.drawable.ic_nav_home:
				getActivityFace().switchFragment(new HomeTabsFragment());
				break;
			case R.drawable.ic_nav_friends:
				getActivityFace().switchFragment(new FriendsFragment());
				break;
		}

		getActivityFace().toggleMenu(SlidingMenu.LEFT);
	}


	private class NavigationMenuItem {
		public String tag;
		public int iconRes;
		public boolean selected;

		public NavigationMenuItem(String tag, int iconRes) {
			this.tag = tag;
			this.iconRes = iconRes;
		}
	}

	private class NewNavigationMenuAdapter extends ItemsAdapter<NavigationMenuItem> {

		public NewNavigationMenuAdapter(Context context, List<NavigationMenuItem> menuItems) {
			super(context, menuItems);
		}

		@Override
		protected View createView(ViewGroup parent) {
			return inflater.inflate(R.layout.new_navigation_menu_item, parent, false);
		}

		@Override
		protected void bindView(NavigationMenuItem item, int pos, View convertView) {
			ImageView icon = (ImageView) convertView.findViewById(R.id.row_icon);
			icon.setImageResource(item.iconRes);
			TextView title = (TextView) convertView.findViewById(R.id.row_title);
			title.setText(item.tag);

			if (pos == UPGRADE_POS){
				if (item.selected)
					convertView.setBackgroundResource(R.drawable.upgrade_menu_item_back_selected);
				else {
					convertView.setBackgroundResource(R.drawable.upgrade_menu_item_back_selector);
				}
			} else {
				icon.setBackgroundDrawable(null);
				if (item.selected) {
					convertView.setBackgroundResource(R.drawable.nav_menu_item_selected);
				} else {
					convertView.setBackgroundResource(R.drawable.nav_menu_item_selector);
				}
			}

		}

		public Context getContext() {
			return context;
		}
	}
}