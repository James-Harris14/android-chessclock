package com.chess.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import com.bugsense.trace.BugSenseHandler;
import com.chess.R;
import com.chess.backend.statics.AppConstants;
import com.chess.backend.statics.AppData;
import com.chess.backend.statics.FlurryData;
import com.chess.backend.statics.StaticData;
import com.chess.model.PopupItem;
import com.chess.ui.fragments.PopupDialogFragment;
import com.chess.ui.fragments.PopupProgressFragment;
import com.chess.ui.interfaces.PopupDialogFace;
import com.chess.ui.views.BackgroundChessDrawable;
import com.chess.utilities.AppUtils;
import com.flurry.android.FlurryAgent;

import java.util.*;

/**
 * BaseFragmentActivity class
 *
 * @author alien_roger
 * @created at: 07.07.12 6:42
 */
public abstract class BaseFragmentActivity extends FragmentActivity implements PopupDialogFace {

	private static final String INFO_POPUP_TAG = "information popup";
	private static final String PROGRESS_TAG = "progress dialog popup";
	protected static final String NETWORK_CHECK_TAG = "network check popup";
	protected static final int NETWORK_REQUEST = 3456;
	protected static final String RE_LOGIN_TAG = "re-login popup";
	protected static final String CHESS_NO_ACCOUNT_TAG = "chess no account popup";
	protected static final String CHECK_UPDATE_TAG = "check update";


	protected DisplayMetrics metrics;
	protected BackgroundChessDrawable backgroundChessDrawable;

	private Context context;
	protected SharedPreferences preferences;
	protected SharedPreferences.Editor preferencesEditor;

	protected PopupItem popupItem;
	protected PopupItem popupProgressItem;
	protected List<PopupDialogFragment> popupManager;
	protected List<PopupProgressFragment> popupProgressManager;

	protected boolean isPaused;
    private String currentLocale;
//	protected Animation fadeInAnimation;
//	protected Animation fadeOutAnimation;

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
		getWindow().setFormat(PixelFormat.RGBA_8888);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(0 == (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)){ // if not debuggable
			try {
				BugSenseHandler.initAndStartSession(this, AppConstants.BUGSENSE_API_KEY);
			} catch (Exception e) {
				Map<String, String> params = new HashMap<String, String>();
				params.put(AppConstants.EXCEPTION, android.os.Build.MODEL + " " + e.toString());
				FlurryAgent.logEvent(FlurryData.BUGSENSE_INIT_EXCEPTION, params);
			}
		}

		context = this;
		backgroundChessDrawable = new BackgroundChessDrawable(this);


		popupItem = new PopupItem();
		popupProgressItem = new PopupItem();

		popupManager = new ArrayList<PopupDialogFragment>();
		popupProgressManager = new ArrayList<PopupProgressFragment>();

		preferences = AppData.getPreferences(this); // TODO rework shared pref usage to unique get method
		preferencesEditor = preferences.edit();

		metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

        currentLocale = preferences.getString(AppConstants.CURRENT_LOCALE, StaticData.LOCALE_EN);
        setLocale();

//		fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in); // temporary unused
//		fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
	}

	@Override
	protected void onResume() {
		super.onResume();
		isPaused = false;

		if (preferences.getLong(AppConstants.FIRST_TIME_START, 0) == 0) {
			preferencesEditor.putLong(AppConstants.FIRST_TIME_START, System.currentTimeMillis());
			preferencesEditor.putInt(AppConstants.ADS_SHOW_COUNTER, 0);
			preferencesEditor.commit();
		}

		if(!currentLocale.equals(getResources().getConfiguration().locale.getLanguage())){
			restartActivity();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		isPaused = true;
	}

	@Override
	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, FlurryData.API_KEY);
	}

	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

    protected void setLocale(){
        String prevLang = getResources().getConfiguration().locale.getLanguage();
		Log.d("TEST","prevLang = " + prevLang);
        String[] languageCodes = getResources().getStringArray(R.array.languages_codes);

        String setLocale = languageCodes[AppData.getLanguageCode(context)];
		Log.d("TEST","setLocale = " + setLocale);
		if(!prevLang.equals(setLocale)) {
            Locale locale = new Locale(setLocale);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());

            preferencesEditor.putString(AppConstants.CURRENT_LOCALE, setLocale);
            preferencesEditor.commit();

            currentLocale = setLocale;

            restartActivity();
        }
    }

    protected void restartActivity(){
        Intent intent = getIntent();
        finish();
        startActivity(intent);
		Log.d("TEST", "___restartActivity___");
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		//first saving my state, so the bundle wont be empty.
		//http://code.google.com/p/android/issues/detail?id=19917
		outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
		super.onSaveInstanceState(outState);
	}

	protected List<String> getItemsFromEntries(int entries){
		String[] array = getResources().getStringArray(entries);
		return getItemsFromArray(array);
	}

	protected List<String> getItemsFromArray(String[] array){
		List<String> items = new ArrayList<String>();
		items.addAll(Arrays.asList(array));
		return items;
	}

	@Override
	public void onPositiveBtnClick(DialogFragment fragment) {
		dismissFragmentDialog(fragment);
	}

	@Override
	public void onNeutralBtnCLick(DialogFragment fragment) {
		dismissFragmentDialog(fragment);
	}

	@Override
	public void onNegativeBtnClick(DialogFragment fragment) {
		dismissFragmentDialog(fragment);
	}

	private void dismissFragmentDialog(DialogFragment fragment){
		popupItem.setPositiveBtnId(R.string.ok);
		popupItem.setNegativeBtnId(R.string.cancel);
		fragment.setCancelable(true);
		fragment.dismiss();
	}

	public void showKeyBoard(EditText editText){
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
	}

	public void hideKeyBoard(View editText){
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
	}

	public void hideKeyBoard(){
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(findViewById(R.id.mainView).getWindowToken(), 0);
	}

	protected void showToast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	protected void showToast(int msgId) {
		Toast.makeText(this, msgId, Toast.LENGTH_SHORT).show();
	}

	// Single button no callback dialogs
	protected void showSinglePopupDialog(int titleId, int messageId) {
		showPopupDialog(titleId, messageId, INFO_POPUP_TAG);
		getLastPopupFragment().setButtons(1);
	}

	protected void showSinglePopupDialog(String title, String message) {
		showPopupDialog(title, message, INFO_POPUP_TAG);
		getLastPopupFragment().setButtons(1);
	}

	protected void showSinglePopupDialog(int titleId, String message) {
		// temporary handling i18n manually
		final String messageI18n = AppUtils.getI18nStringForAPIError(context, message);
		showPopupDialog(titleId, messageI18n, INFO_POPUP_TAG);
		getLastPopupFragment().setButtons(1);
	}

	protected void showSinglePopupDialog(String message) {
		showPopupDialog(message, INFO_POPUP_TAG);
		getLastPopupFragment().setButtons(1);
	}

	protected void showSinglePopupDialog(int messageId) {
		showPopupDialog(messageId, INFO_POPUP_TAG);
		getLastPopupFragment().setButtons(1);
	}

	// Default Dialogs
	protected void showPopupDialog(int titleId, int messageId, String tag) {
		popupItem.setTitle(titleId);
		popupItem.setMessage(messageId);
		updatePopupAndShow(tag);
	}

	protected void showPopupDialog(int titleId, String messageId, String tag) {
		popupItem.setTitle(titleId);
		popupItem.setMessage(messageId);
		updatePopupAndShow(tag);
	}

	protected void showPopupDialog(String title, String message, String tag) {
		popupItem.setTitle(title);
		popupItem.setMessage(message);
		updatePopupAndShow(tag);
	}

	protected void showPopupDialog(int titleId, String tag) {
		popupItem.setTitle(titleId);
		popupItem.setMessage(StaticData.SYMBOL_EMPTY);
		updatePopupAndShow(tag);
	}

	protected void showPopupDialog(String title, String tag) {  // TODO handle popups overlays - set default button values
		popupItem.setTitle(title);
		popupItem.setMessage(StaticData.SYMBOL_EMPTY);
		updatePopupAndShow(tag);
	}

	private synchronized void updatePopupAndShow(String tag){
		popupManager.add(PopupDialogFragment.newInstance(popupItem));
		getLastPopupFragment().show(getSupportFragmentManager(), tag);
	}

	// Progress Dialogs
	protected void showPopupProgressDialog(String title) {
		popupProgressItem.setTitle(title);
		popupProgressItem.setMessage(StaticData.SYMBOL_EMPTY);
		PopupProgressFragment popupProgressDialogFragment = PopupProgressFragment.newInstance(popupItem);
		updateProgressAndShow(popupProgressDialogFragment);
	}

	protected void showPopupProgressDialog(String title, String message) {
		popupProgressItem.setTitle(title);
		popupProgressItem.setMessage(message);
		PopupProgressFragment popupProgressDialogFragment = PopupProgressFragment.newInstance(popupItem);
		updateProgressAndShow(popupProgressDialogFragment);
	}

	protected void showPopupProgressDialog(int titleId) {
		popupProgressItem.setTitle(titleId);
		popupProgressItem.setMessage(StaticData.SYMBOL_EMPTY);
		PopupProgressFragment popupProgressDialogFragment = PopupProgressFragment.newInstance(popupItem);
		updateProgressAndShow(popupProgressDialogFragment);
	}

	protected void showPopupHardProgressDialog(int titleId) {
		popupProgressItem.setTitle(titleId);
		popupProgressItem.setMessage(StaticData.SYMBOL_EMPTY);
		PopupProgressFragment popupProgressDialogFragment = PopupProgressFragment.newInstance(popupItem);
		popupProgressDialogFragment.setNotCancelable();
		updateProgressAndShow(popupProgressDialogFragment);
	}

	protected void showPopupProgressDialog(int titleId, int messageId) {
		popupProgressItem.setTitle(titleId);
		popupProgressItem.setMessage(messageId);
		PopupProgressFragment popupProgressDialogFragment = PopupProgressFragment.newInstance(popupItem);
		updateProgressAndShow(popupProgressDialogFragment);
	}

	private void updateProgressAndShow(PopupProgressFragment popupProgressDialogFragment){
		popupProgressDialogFragment.updatePopupItem(popupProgressItem);
		popupProgressDialogFragment.show(getSupportFragmentManager(), PROGRESS_TAG);
		popupProgressManager.add(popupProgressDialogFragment);
	}

	protected void dismissFragmentDialog() {
		if (getLastPopupFragment() == null)
			return;

		getLastPopupFragment().dismiss();
		popupManager.remove(popupManager.size()-1);
	}

	protected PopupDialogFragment getLastPopupFragment(){
		if (popupManager.size() == 0){
			return null; //
		} else {
			return popupManager.get(popupManager.size()-1);
		}
	}

	protected void dismissProgressDialog() {
		if(popupProgressManager.size() == 0)
			return;

		popupProgressManager.get(popupProgressManager.size()-1).dismiss();
	}

	public void dismissAllPopups() {
		for (PopupDialogFragment fragment : popupManager) {
			fragment.dismiss();
		}
	}

	protected String getTextFromField(EditText editText) {
		return editText.getText().toString().trim();
	}

	protected Context getContext() {
		return context;
	}
}
