package com.quran.labs.androidquran.widgets;

import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.data.Constants;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.util.QuranUtils;
import com.quran.labs.androidquran.widgets.spinner.AdapterViewCompat;
import com.quran.labs.androidquran.widgets.spinner.SpinnerCompat;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AudioStatusBar extends LinearLayout {

  public static final int STOPPED_MODE = 1;
  public static final int DOWNLOADING_MODE = 2;
  public static final int PLAYING_MODE = 3;
  public static final int PAUSED_MODE = 4;
  public static final int PROMPT_DOWNLOAD_MODE = 5;

  private Context mContext;
  private int mCurrentMode;
  private int mButtonWidth;
  private int mSeparatorWidth;
  private int mSeparatorSpacing;
  private int mTextFontSize;
  private int mTextFullFontSize;
  private int mSpinnerPadding;

  private int mCurrentQari;
  private int mCurrentRepeat = 0;
  private boolean mIsRtl;
  private boolean mHasErrorText;
  private boolean mHaveCriticalError = false;
  private SharedPreferences mSharedPreferences;

  private SpinnerCompat mSpinner;
  private TextView mProgressText;
  private ProgressBar mProgressBar;
  private TextView mRepeatButton;
  private AudioBarListener mAudioBarListener;

  private int[] mRepeatValues = {0, 1, 2, 3, -1};

  public interface AudioBarListener {

    void onPlayPressed();

    void onPausePressed();

    void onNextPressed();

    void onPreviousPressed();

    void onStopPressed();

    void onCancelPressed(boolean stopDownload);

    void setRepeatCount(int repeatCount);

    void onAcceptPressed();

    void onAudioSettingsPressed();
  }

  public AudioStatusBar(Context context) {
    super(context);
    init(context);
  }

  public AudioStatusBar(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public AudioStatusBar(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  private void init(Context context) {
    mContext = context;
    Resources resources = getResources();
    mButtonWidth = resources.getDimensionPixelSize(
        R.dimen.audiobar_button_width);
    mSeparatorWidth = resources.getDimensionPixelSize(
        R.dimen.audiobar_separator_width);
    mSeparatorSpacing = resources.getDimensionPixelSize(
        R.dimen.audiobar_separator_padding);
    mTextFontSize = resources.getDimensionPixelSize(
        R.dimen.audiobar_text_font_size);
    mTextFullFontSize = resources.getDimensionPixelSize(
        R.dimen.audiobar_text_full_font_size);
    mSpinnerPadding = resources
        .getDimensionPixelSize(R.dimen.audiobar_spinner_padding);
    setOrientation(LinearLayout.HORIZONTAL);

    // only flip the layout when the language is rtl and we're on api 17+
    mIsRtl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
        (QuranSettings.getInstance(mContext).isArabicNames() || QuranUtils.isRtl());
    mSharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(context.getApplicationContext());
    mCurrentQari = mSharedPreferences.getInt(
        Constants.PREF_DEFAULT_QARI, 0);
    showStoppedMode();
  }

  public int getCurrentMode() {
    return mCurrentMode;
  }

  public void switchMode(int mode) {
    if (mode == mCurrentMode) {
      return;
    }

    if (mode == STOPPED_MODE) {
      showStoppedMode();
    } else if (mode == PROMPT_DOWNLOAD_MODE) {
      showPromptForDownloadMode();
    } else if (mode == DOWNLOADING_MODE) {
      showDownloadingMode();
    } else if (mode == PLAYING_MODE) {
      showPlayingMode(false);
    } else {
      showPlayingMode(true);
    }
  }

  public int getCurrentQari() {
    if (mSpinner != null) {
      return mSpinner.getSelectedItemPosition();
    }
    return mCurrentQari;
  }

  public void updateSelectedItem() {
    if (mSpinner != null) {
      mSpinner.setSelection(mCurrentQari);
    }
  }

  public void setProgress(int progress) {
    if (mHasErrorText) {
      mProgressText.setText(R.string.downloading_title);
      mHasErrorText = false;
    }

    if (mProgressBar != null) {
      if (progress >= 0) {
        mProgressBar.setIndeterminate(false);
        mProgressBar.setProgress(progress);
        mProgressBar.setMax(100);
      } else {
        mProgressBar.setIndeterminate(true);
      }
    }
  }

  public void setProgressText(String progressText, boolean isCriticalError) {
    if (mProgressText != null) {
      mHasErrorText = true;
      mProgressText.setText(progressText);
      if (isCriticalError && mProgressBar != null) {
        mProgressBar.setVisibility(View.GONE);
        mProgressText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
            mTextFullFontSize);
        mHaveCriticalError = true;
      }
    }
  }

  private void showStoppedMode() {
    mCurrentMode = STOPPED_MODE;
    removeAllViews();

    if (mIsRtl) {
      addSpinner();
      addSeparator();
      addButton(R.drawable.ic_play, false);
    } else {
      addButton(R.drawable.ic_play, false);
      addSeparator();
      addSpinner();
    }
  }

  private void addSpinner() {
    if (mSpinner == null) {
      mSpinner = new SpinnerCompat(mContext, null,
          R.attr.actionDropDownStyle);
      mSpinner.setDropDownVerticalOffset(mSpinnerPadding);
      final ArrayAdapter<CharSequence> adapter =
          ArrayAdapter.createFromResource(mContext,
              R.array.quran_readers_name,
              R.layout.sherlock_spinner_item);

      adapter.setDropDownViewResource(
          R.layout.sherlock_spinner_dropdown_item);
      mSpinner.setAdapter(adapter);

      mSpinner.setOnItemSelectedListener(
          new AdapterViewCompat.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterViewCompat<?> parent,
                View view, int position, long id) {
              if (position != mCurrentQari) {
                mSharedPreferences.edit().
                    putInt(Constants.PREF_DEFAULT_QARI,
                        position).apply();
                mCurrentQari = position;
              }
            }

            @Override
            public void onNothingSelected(AdapterViewCompat<?> parent) {
            }
          });
    }
    mSpinner.setSelection(mCurrentQari);
    final LayoutParams params = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
    params.weight = 1;
    if (mIsRtl) {
      ViewCompat.setLayoutDirection(mSpinner, ViewCompat.LAYOUT_DIRECTION_RTL);
      params.leftMargin = mSpinnerPadding;
    } else {
      params.rightMargin = mSpinnerPadding;
    }
    addView(mSpinner, params);
  }

  private void showPromptForDownloadMode() {
    mCurrentMode = PROMPT_DOWNLOAD_MODE;

    removeAllViews();

    if (mIsRtl) {
      addButton(R.drawable.ic_cancel, false);
      addDownloadOver3gPrompt();
      addSeparator();
      addButton(R.drawable.ic_accept, false);
    } else {
      addButton(R.drawable.ic_accept, false);
      addSeparator();
      addDownloadOver3gPrompt();
      addButton(R.drawable.ic_cancel, false);
    }
  }

  private void addDownloadOver3gPrompt() {
    TextView mPromptText = new TextView(mContext);
    mPromptText.setTextColor(Color.WHITE);
    mPromptText.setGravity(Gravity.CENTER_VERTICAL);
    mPromptText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
        mTextFontSize);
    mPromptText.setText(R.string.download_non_wifi_prompt);
    LayoutParams params = new LayoutParams(0,
        LayoutParams.MATCH_PARENT);
    params.weight = 1;
    addView(mPromptText, params);
  }

  private void showDownloadingMode() {
    mCurrentMode = DOWNLOADING_MODE;

    removeAllViews();

    if (mIsRtl) {
      addDownloadProgress();
      addSeparator();
      addButton(R.drawable.ic_cancel, false);
    } else {
      addButton(R.drawable.ic_cancel, false);
      addSeparator();
      addDownloadProgress();
    }
  }

  private void addDownloadProgress() {
    LinearLayout ll = new LinearLayout(mContext);
    ll.setOrientation(LinearLayout.VERTICAL);

    mProgressBar = (ProgressBar) LayoutInflater.from(mContext)
        .inflate(R.layout.download_progress_bar, this, false);
    mProgressBar.setIndeterminate(true);
    mProgressBar.setVisibility(View.VISIBLE);

    ll.addView(mProgressBar, LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT);

    mProgressText = new TextView(mContext);
    mProgressText.setTextColor(Color.WHITE);
    mProgressText.setGravity(Gravity.CENTER_VERTICAL);
    mProgressText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
        mTextFontSize);
    mProgressText.setText(R.string.downloading_title);

    ll.addView(mProgressText, LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT);

    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT);
    lp.weight = 1;
    lp.setMargins(mSeparatorSpacing, 0, mSeparatorSpacing, 0);
    if (mIsRtl) {
      lp.leftMargin = mSpinnerPadding;
    } else {
      lp.rightMargin = mSpinnerPadding;
    }
    addView(ll, lp);
  }

  private void showPlayingMode(boolean isPaused) {
    removeAllViews();

    final boolean withWeight = false;

    int button;
    if (isPaused) {
      button = R.drawable.ic_play;
      mCurrentMode = PAUSED_MODE;
    } else {
      button = R.drawable.ic_pause;
      mCurrentMode = PLAYING_MODE;
    }

    addButton(R.drawable.ic_stop, withWeight);
    addButton(R.drawable.ic_previous, withWeight);
    addButton(button, withWeight);
    addButton(R.drawable.ic_next, withWeight);

    mRepeatButton = new TextView(mContext);
    mRepeatButton.setCompoundDrawablesWithIntrinsicBounds(
        R.drawable.ic_repeat, 0, 0, 0);
    mRepeatButton.setBackgroundResource(
        R.drawable.abc_item_background_holo_dark);
    mRepeatButton.setTag(R.drawable.ic_repeat);
    mRepeatButton.setOnClickListener(mOnClickListener);
    updateRepeatButtonText();
    addView(mRepeatButton, LayoutParams.WRAP_CONTENT,
        LayoutParams.MATCH_PARENT);

    addButton(R.drawable.ic_action_settings, false);
  }

  private void addButton(int imageId, boolean withWeight) {
    ImageView button = new ImageView(mContext);
    button.setImageResource(imageId);
    button.setScaleType(ImageView.ScaleType.CENTER);
    button.setOnClickListener(mOnClickListener);
    button.setTag(imageId);
    button.setBackgroundResource(R.drawable.abc_item_background_holo_dark);
    final LayoutParams params = new LayoutParams(
        withWeight ? 0 : mButtonWidth, LayoutParams.MATCH_PARENT);
    if (withWeight) {
      params.weight = 1;
    }
    addView(button, params);
  }

  private void addSeparator() {
    ImageView separator = new ImageView(mContext);
    separator.setBackgroundColor(Color.WHITE);
    separator.setPadding(0, mSeparatorSpacing, 0, mSeparatorSpacing);
    LinearLayout.LayoutParams paddingParams =
        new LayoutParams(mSeparatorWidth, LayoutParams.MATCH_PARENT);

    final int right = mIsRtl ? 0 : mSeparatorSpacing;
    final int left = mIsRtl ? mSeparatorSpacing : 0;
    paddingParams.setMargins(left, 0, right, 0);
    addView(separator, paddingParams);
  }

  private void incrementRepeat() {
    mCurrentRepeat++;
    if (mCurrentRepeat == mRepeatValues.length) {
      mCurrentRepeat = 0;
    }
    updateRepeatButtonText();
  }

  private void updateRepeatButtonText() {
    String str;
    int value = mRepeatValues[mCurrentRepeat];
    if (value == 0) {
      str = "";
    } else if (value > 0) {
      str = mRepeatValues[mCurrentRepeat] + "";
    } else {
      str = mContext.getString(R.string.infinity);
    }
    mRepeatButton.setText(str);
  }

  public void setRepeatCount(int repeatCount) {
    boolean updated = false;
    for (int i = 0; i < mRepeatValues.length; i++) {
      if (mRepeatValues[i] == repeatCount) {
        if (mCurrentRepeat != i) {
          mCurrentRepeat = i;
          updated = true;
        }
        break;
      }
    }

    if (updated && mRepeatButton != null) {
      updateRepeatButtonText();
    }
  }

  public void setAudioBarListener(AudioBarListener listener) {
    mAudioBarListener = listener;
  }

  OnClickListener mOnClickListener = new OnClickListener() {
    @Override
    public void onClick(View view) {
      if (mAudioBarListener != null) {
        int tag = (Integer) view.getTag();
        switch (tag) {
          case R.drawable.ic_play:
            mAudioBarListener.onPlayPressed();
            break;
          case R.drawable.ic_stop:
            mAudioBarListener.onStopPressed();
            break;
          case R.drawable.ic_pause:
            mAudioBarListener.onPausePressed();
            break;
          case R.drawable.ic_next:
            mAudioBarListener.onPreviousPressed();
            break;
          case R.drawable.ic_previous:
            mAudioBarListener.onNextPressed();
            break;
          case R.drawable.ic_repeat:
            incrementRepeat();
            mAudioBarListener.setRepeatCount(
                mRepeatValues[mCurrentRepeat]);
            break;
          case R.drawable.ic_cancel:
            if (mHaveCriticalError) {
              mHaveCriticalError = false;
              switchMode(STOPPED_MODE);
            } else {
              mAudioBarListener.onCancelPressed(
                  mCurrentMode != PROMPT_DOWNLOAD_MODE);
            }
            break;
          case R.drawable.ic_accept:
            mAudioBarListener.onAcceptPressed();
            break;
          case R.drawable.ic_action_settings:
            mAudioBarListener.onAudioSettingsPressed();
            break;
        }
      }
    }
  };
}
