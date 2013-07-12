package com.chess.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.Menu;
import com.chess.R;
import com.chess.backend.statics.AppData;
import com.chess.backend.statics.IntentConstants;
import com.chess.ui.fragments.BasePopupsFragment;
import com.chess.ui.fragments.CommonLogicFragment;
import com.chess.ui.fragments.home.HomeTabsFragment;
import com.chess.ui.fragments.live.GameLiveFragment;
import com.chess.ui.fragments.live.LiveGameWaitFragment;
import com.chess.ui.fragments.upgrade.UpgradeDetailsFragment;
import com.chess.ui.fragments.welcome.WelcomeFragment;
import com.chess.ui.fragments.welcome.WelcomeTabsFragment;
import com.chess.ui.interfaces.ActiveFragmentInterface;
import com.slidingmenu.lib.SlidingMenu;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 30.12.12
 * Time: 13:37
 */
public class MainFragmentFaceActivity extends LiveBaseActivity implements ActiveFragmentInterface {

	private static final String SHOW_ACTION_BAR = "show_actionbar_in_activity";

	private Fragment currentActiveFragment;
	private Hashtable<Integer, Integer> badgeItems;
	private SlidingMenu slidingMenu;
	private List<SlidingMenu.OnOpenedListener> openMenuListeners;
	private boolean showActionBar;
	private int customActionBarViewId;
	private AppData appData;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setSlidingActionBarEnabled(true);

		setContentView(R.layout.new_main_active_screen);

		customActionBarViewId = R.layout.new_custom_actionbar;

		openMenuListeners = new ArrayList<SlidingMenu.OnOpenedListener>();

		if (savedInstanceState == null) {
			// set the Above View
			if (!TextUtils.isEmpty(getAppData().getUserToken())) { // if user have login token already
				switchFragment(new HomeTabsFragment());
//				switchFragment(new VideosFragment());
				showActionBar = true;
			} else {
				switchFragment(new WelcomeTabsFragment());
				showActionBar = false;
			}
		} else { // fragments state will be automatically restored
			showActionBar = savedInstanceState.getBoolean(SHOW_ACTION_BAR);
		}

		slidingMenu = getSlidingMenu();
		slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
		slidingMenu.setOnOpenedListener(openMenuListener);
		slidingMenu.setOnCloseListener(closeMenuListener);

		badgeItems = new Hashtable<Integer, Integer>();

		int id = getAppData().getThemeBackId();
		getWindow().setBackgroundDrawableResource(id);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		getActionBarHelper().setCustomView(customActionBarViewId);
		super.onPostCreate(savedInstanceState);

		getActionBarHelper().showActionBar(showActionBar);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (intent.hasExtra(IntentConstants.LIVE_CHESS)) {

			GameLiveFragment gameLiveFragment = (GameLiveFragment) findFragmentByTag(GameLiveFragment.class.getSimpleName());
			if (gameLiveFragment != null) {
				switchFragment(gameLiveFragment);
				return;
			}
			LiveGameWaitFragment fragmentByTag = (LiveGameWaitFragment) findFragmentByTag(LiveGameWaitFragment.class.getSimpleName());
			if (fragmentByTag == null) {
				fragmentByTag = new LiveGameWaitFragment();
			}

			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			currentActiveFragment = fragmentByTag;

			ft.replace(R.id.content_frame, fragmentByTag, fragmentByTag.getClass().getSimpleName());
			ft.commitAllowingStateLoss();
		}

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SHOW_ACTION_BAR, showActionBar);

	}

	@Override
	public void updateActionBarIcons() {
		if (!HONEYCOMB_PLUS_API) {
			adjustActionBar();
		}
	}

	@Override
	public void setTitle(int titleId) {
		getActionBarHelper().setTitle(titleId);
	}

	@Override
	public void updateTitle(int titleId) {
		if (!HONEYCOMB_PLUS_API) { // set title before custom view for pre-HC
			getActionBarHelper().setTitle(titleId);
		}
		getActionBarHelper().setCustomView(R.layout.new_custom_actionbar);
		getActionBarHelper().setTitle(titleId);

		if (!HONEYCOMB_PLUS_API) {
			for (Map.Entry<Integer, Integer> entry : badgeItems.entrySet()) {
				getActionBarHelper().setBadgeValueForId(entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public void setTitlePadding(int padding) {
		getActionBarHelper().setTitlePadding(padding);
	}

	@Override
	public void showActionBar(boolean show) {
		showActionBar = show;
		getActionBarHelper().showActionBar(show);
	}

	@Override
	public void setMainBackground(int drawableThemeId) {
		getAppData().setThemeBackId(drawableThemeId);
		getWindow().setBackgroundDrawableResource(drawableThemeId);
		setBackToDecorChild(drawableThemeId);
	}

	@Override
	public void showActionMenu(int menuId, boolean show) {
		enableActionMenu(menuId, show);
	}

	@Override
	public void setCustomActionBarViewId(int viewId) {
		customActionBarViewId = viewId;
		getActionBarHelper().setCustomView(customActionBarViewId);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	private SlidingMenu.OnOpenedListener openMenuListener = new SlidingMenu.OnOpenedListener() {
		@Override
		public void onOpened() { // Don't remove reuse later
			if (slidingMenu.isSecondaryMenuShowing()) {
				for (SlidingMenu.OnOpenedListener openedListener : openMenuListeners) { // Inform listeners inside fragments
					openedListener.onOpenedRight();
				}
			}
		}

		@Override
		public void onOpenedRight() {
		}
	};

	private SlidingMenu.OnCloseListener closeMenuListener = new SlidingMenu.OnCloseListener() {
		@Override
		public void onClose() {
			hideKeyBoard();
		}
	};

	@Override
	public void addOnOpenMenuListener(SlidingMenu.OnOpenedListener listener) {
		if (openMenuListeners != null)
			openMenuListeners.add(listener);
	}

	@Override
	public void removeOnOpenMenuListener(SlidingMenu.OnOpenedListener listener) {
		if (openMenuListeners != null)
			openMenuListeners.remove(listener);
	}

	@Override
	public void registerGcm() {
		registerGcmService();
	}

	@Override
	public void unRegisterGcm() {
		unRegisterGcmService();
	}

	@Override
	public void changeRightFragment(CommonLogicFragment fragment) {
		// set right menu. Left Menu is already set in BaseActivity
		SlidingMenu sm = getSlidingMenu();
		sm.setMode(SlidingMenu.LEFT_RIGHT);
		sm.setBehindRightOffsetRes(R.dimen.slidingmenu_offset_right);
		sm.setSecondaryMenu(R.layout.slide_menu_right_frame);
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.menu_frame_right, fragment)
				.commit();
		sm.setSecondaryShadowDrawable(R.drawable.defaultshadowright);
		sm.setShadowDrawable(R.drawable.defaultshadow);
//		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
	}

	@Override
	public void changeLeftFragment(CommonLogicFragment fragment) {
		// change left menu fragment
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		leftMenuFragment = fragment;
		ft.replace(R.id.menu_frame_left, leftMenuFragment);
		ft.commit();
	}

	@Override
	public void setTouchModeToSlidingMenu(int touchMode) {
		SlidingMenu sm = getSlidingMenu();
		sm.setTouchModeAbove(touchMode);
	}

	@Override
	public void openFragment(BasePopupsFragment fragment) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		currentActiveFragment = fragment;

		ft.replace(R.id.content_frame, fragment, fragment.getClass().getSimpleName());
		ft.addToBackStack(fragment.getClass().getSimpleName());
		ft.commit();
	}

	@Override
	public void openFragment(BasePopupsFragment fragment, int code) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		currentActiveFragment = fragment;

		ft.replace(R.id.content_frame, fragment, fragment.getClass().getSimpleName());
		ft.addToBackStack(fragment.getClass().getSimpleName());
		ft.commit();
	}

	@Override
	public void switchFragment(BasePopupsFragment fragment) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		currentActiveFragment = fragment;

		ft.replace(R.id.content_frame, fragment, fragment.getClass().getSimpleName());
		ft.commit();
	}

	@Override
	public void switchFragment(BasePopupsFragment fragment, int code) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		currentActiveFragment = fragment;

		ft.replace(R.id.content_frame, fragment, fragment.getClass().getSimpleName());
		ft.commit();
	}

	@Override
	public void toggleLeftMenu() {
		toggleMenu(SlidingMenu.LEFT);
	}

	@Override
	public void toggleRightMenu() {
		toggleMenu(SlidingMenu.RIGHT);
	}

	private void toggleMenu(int code) {
		switch (code) {
			case SlidingMenu.LEFT:
				if (getSlidingMenu().isMenuShowing()) {
					getSlidingMenu().toggle();
				} else {
					getSlidingMenu().showMenu();
				}
				break;
			case SlidingMenu.RIGHT:
				boolean visible = getSlidingMenu().isMenuShowing();
				if (visible) {
					getSlidingMenu().toggle();
				} else {
					getSlidingMenu().showSecondaryMenu();
				}
				break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == UpgradeDetailsFragment.RC_REQUEST) {
			FragmentManager fragmentManager = getSupportFragmentManager(); // the only one way to call it after startIntentSenderForResult
			Fragment fragment = fragmentManager.findFragmentByTag(UpgradeDetailsFragment.class.getSimpleName());
			if (fragment != null) {
				fragment.onActivityResult(requestCode, resultCode,data);
			}
		}
	}

	@Override
	public void onBackPressed() {
		// there is no way to handle backPressed from fragment, so do this non OOP solution. Google, why are you so evil
		WelcomeFragment welcomeFragment = (WelcomeFragment) getSupportFragmentManager().findFragmentByTag(WelcomeFragment.class.getSimpleName());
		int orientation = getResources().getConfiguration().orientation;
		if (welcomeFragment != null && orientation == Configuration.ORIENTATION_LANDSCAPE) {
			if (welcomeFragment.hideYoutubeFullScreen()) {
				return;
			}
		} else if (welcomeFragment != null) { // if swipe to registration screen, handle back button to swipe back
			if (welcomeFragment.swipeBackFromSignUp()) {
				return;
			}
		}
		WelcomeTabsFragment welcomeTabsFragment = (WelcomeTabsFragment) getSupportFragmentManager().findFragmentByTag(WelcomeTabsFragment.class.getSimpleName());
		if (welcomeTabsFragment != null && welcomeTabsFragment.showPreviousFragment()) {
			return;
		}
		showPreviousFragment();
	}

	@Override
	public void showPreviousFragment() {
		boolean fragmentsLeft = getSupportFragmentManager().popBackStackImmediate();
		if (!fragmentsLeft) {
			super.onBackPressed();
		}
	}

	@Override
	public void clearFragmentStack() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		int count = fragmentManager.getBackStackEntryCount();
		if (count > 0) {
			int firstFragmentId = fragmentManager.getBackStackEntryAt(0).getId();
			fragmentManager.popBackStack(firstFragmentId, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	@Override
	public void setBadgeValueForId(int menuId, int value) {
		badgeItems.put(menuId, value);
		getActionBarHelper().setBadgeValueForId(menuId, value);
	}

//	@Override
//	public void setBadgeValueForId(int menuId, int value, Menu menu) {
//		badgeItems.put(menuId, value);
//		getActionBarHelper().setBadgeValueForId(menuId, value, menu);
//	}

	@Override
	public CoreActivityActionBar getActionBarActivity() {
		return getInstance();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		boolean displayMenu = super.onCreateOptionsMenu(menu);
		for (Map.Entry<Integer, Integer> entry : badgeItems.entrySet()) {
			getActionBarHelper().setBadgeValueForId(entry.getKey(), entry.getValue(), menu);
		}

		return displayMenu;
	}

	public void startActivityFromFragmentForResult(Intent intent, int requestCode) {
		if (currentActiveFragment != null) {
			startActivityFromFragment(currentActiveFragment, intent, requestCode);
		}
	}

	@Override
	public AppData getMeAppData() {
		return getAppData();
	}

	@Override
	public String getMeUserName() {
		return getCurrentUserName();
	}

	@Override
	public String getMeUserToken() {
		return getCurrentUserToken();
	}

}