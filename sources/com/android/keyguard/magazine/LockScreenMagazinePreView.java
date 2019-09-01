package com.android.keyguard.magazine;

import android.app.ActivityManagerCompat;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.analytics.LockScreenMagazineAnalytics;
import com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo;
import com.android.keyguard.utils.PackageUtils;
import com.android.keyguard.wallpaper.WallpaperAuthorityUtils;
import com.android.systemui.Application;
import com.android.systemui.R;
import com.android.systemui.SystemUICompat;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import java.util.Locale;
import miui.os.Build;

public class LockScreenMagazinePreView extends LinearLayout {
    private TextView mContent;
    private int mDensityDpi;
    private LinearLayout mDesLayout;
    private LinearLayout mDomesticPreLayout;
    private float mFontScale;
    private ImageView mGlobalBottomIcon;
    private ImageView mGlobalBottomLine;
    private TextView mGlobalButton;
    private ImageView mGlobalLeftIcon;
    private LinearLayout mGlobalPreLayout;
    private TextView mGlobalProvider;
    private TextView mGlobalSource;
    private TextView mGlobalTitle;
    private boolean mIsGlobalPreDarkMode;
    private boolean mIsSettingButtonDarkMode;
    private ImageView mLinkButton;
    private View.OnClickListener mLinkClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (LockScreenMagazinePreView.this.openAd()) {
                LockScreenMagazineAnalytics.recordLockScreenMagazinePreviewAction(LockScreenMagazinePreView.this.mContext, "click_link");
            }
        }
    };
    private Object mLocaleList;
    protected LockScreenMagazineWallpaperInfo mLockScreenMagazineWallpaperInfo;
    protected KeyguardUpdateMonitor mMonitor;
    private TextView mPreviewButton;
    private Resources mResources;
    private Button mSettingButton;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private TextView mTitle;
    /* access modifiers changed from: private */
    public RelativeLayout mTitleLayout;
    /* access modifiers changed from: private */
    public float mTitleLayoutWidth;

    public LockScreenMagazinePreView(Context context) {
        super(context);
    }

    public LockScreenMagazinePreView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mResources = context.getResources();
    }

    public void onFinishInflate() {
        this.mTitle = (TextView) findViewById(R.id.title);
        this.mTitle.setTypeface(Typeface.create("miui-bold", 0));
        this.mLinkButton = (ImageView) findViewById(R.id.link);
        this.mContent = (TextView) findViewById(R.id.content);
        this.mContent.setTypeface(Typeface.create("miui-light", 0));
        this.mPreviewButton = (TextView) findViewById(R.id.preview_button);
        this.mPreviewButton.setContentDescription(this.mResources.getText(R.string.accessibility_enter_lock_wallpaper));
        this.mSettingButton = (Button) findViewById(R.id.settings_button);
        initSettingButton();
        this.mDesLayout = (LinearLayout) findViewById(R.id.des_layout);
        this.mTitleLayout = (RelativeLayout) findViewById(R.id.title_layout);
        this.mTitleLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (right - left != oldRight - oldLeft) {
                    float unused = LockScreenMagazinePreView.this.mTitleLayoutWidth = (float) LockScreenMagazinePreView.this.mTitleLayout.getWidth();
                    LockScreenMagazinePreView.this.updateLinkButton();
                }
            }
        });
        this.mGlobalTitle = (TextView) findViewById(R.id.global_title);
        this.mGlobalProvider = (TextView) findViewById(R.id.global_provider);
        this.mGlobalSource = (TextView) findViewById(R.id.global_source);
        this.mGlobalBottomIcon = (ImageView) findViewById(R.id.global_bottom_icon);
        this.mGlobalLeftIcon = (ImageView) findViewById(R.id.global_left_icon);
        this.mGlobalBottomLine = (ImageView) findViewById(R.id.global_bottom_line);
        this.mGlobalButton = (TextView) findViewById(R.id.global_button);
        this.mGlobalButton.setTypeface(Typeface.create("miui-regular", 0));
        this.mGlobalPreLayout = (LinearLayout) findViewById(R.id.wallpaper_global_des);
        this.mDomesticPreLayout = (LinearLayout) findViewById(R.id.wallpaper_domestic_des);
        initLayoutVisibility();
    }

    public void initLayoutVisibility() {
        if (!Build.IS_INTERNATIONAL_BUILD || !this.mMonitor.isSupportLockScreenMagazineLeft()) {
            this.mGlobalPreLayout.setVisibility(8);
            this.mDomesticPreLayout.setVisibility(0);
            return;
        }
        this.mGlobalPreLayout.setVisibility(0);
        this.mDomesticPreLayout.setVisibility(8);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Object localeList = SystemUICompat.getLocales(newConfig);
        float fontScale = newConfig.fontScale;
        int densityDpi = newConfig.densityDpi;
        if (this.mLocaleList != localeList) {
            updateLanguage();
            this.mLocaleList = localeList;
        }
        if (this.mFontScale != fontScale) {
            updateFontScale();
            updateLinkButton();
            this.mFontScale = fontScale;
        }
        if (this.mDensityDpi != densityDpi) {
            updateFontScale();
            updateViewsLayoutParams();
            updateDrawableResource();
            this.mDensityDpi = densityDpi;
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void initSettingButton() {
        new AsyncTask<Void, Void, String>() {
            /* access modifiers changed from: protected */
            public String doInBackground(Void... params) {
                return LockScreenMagazineUtils.getLockScreenMagazineSettingsDeepLink(LockScreenMagazinePreView.this.mContext);
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(String settingsDeepLink) {
                Intent intent = null;
                if (!TextUtils.isEmpty(settingsDeepLink)) {
                    intent = new Intent("android.intent.action.VIEW");
                    intent.putExtra("from", "lks_preview");
                    intent.setData(Uri.parse(settingsDeepLink));
                    intent.addFlags(268435456);
                    intent.addFlags(67108864);
                }
                LockScreenMagazinePreView.this.updateSettingButton(intent);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    /* access modifiers changed from: private */
    public void updateSettingButton(final Intent intent) {
        if (intent == null || PackageUtils.resolveIntent(this.mContext, intent) == null) {
            this.mSettingButton.setVisibility(8);
            MiuiKeyguardUtils.setViewTouchDelegate(this.mSettingButton, 0);
            return;
        }
        this.mSettingButton.setVisibility(0);
        this.mSettingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LockScreenMagazinePreView.this.hideLockScreenInActivityManager();
                StatusBar statusBar = (StatusBar) ((Application) LockScreenMagazinePreView.this.getContext().getApplicationContext()).getSystemUIApplication().getComponent(StatusBar.class);
                if (statusBar != null) {
                    statusBar.startActivity(intent, true);
                }
                LockScreenMagazineAnalytics.recordLockScreenMagazinePreviewAction(LockScreenMagazinePreView.this.mContext, "click_settings");
            }
        });
        this.mSettingButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                return true;
            }
        });
        MiuiKeyguardUtils.setViewTouchDelegate(this.mSettingButton, 50);
    }

    public void hideLockScreenInActivityManager() {
        if (Build.VERSION.SDK_INT < 26) {
            try {
                ActivityManagerCompat.setLockScreenShown(false, true);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateLanguage() {
        updateViews();
    }

    private void updateFontScale() {
        if (!miui.os.Build.IS_INTERNATIONAL_BUILD || !this.mMonitor.isSupportLockScreenMagazineLeft()) {
            this.mTitle.setTextSize(0, (float) this.mResources.getDimensionPixelSize(R.dimen.lock_screen_magazine_pre_title_text_size));
            this.mContent.setTextSize(0, (float) this.mResources.getDimensionPixelSize(R.dimen.lock_screen_magazine_pre_content_text_size));
            this.mPreviewButton.setTextSize(0, (float) this.mResources.getDimensionPixelSize(R.dimen.lock_screen_magazine_pre_button_text_size));
            return;
        }
        this.mGlobalTitle.setTextSize(0, (float) this.mResources.getDimensionPixelSize(R.dimen.lock_screen_magazine_pre_global_title_text_size));
        this.mGlobalProvider.setTextSize(0, (float) this.mResources.getDimensionPixelSize(R.dimen.lock_screen_magazine_pre_global_content_text_size));
        this.mGlobalSource.setTextSize(0, (float) this.mResources.getDimensionPixelSize(R.dimen.lock_screen_magazine_pre_global_content_text_size));
        this.mGlobalButton.setTextSize(0, (float) this.mResources.getDimensionPixelSize(R.dimen.lock_screen_magazine_pre_global_button_text_size));
    }

    private void updateDrawableResource() {
        int i;
        int i2;
        int i3;
        int i4;
        if (!miui.os.Build.IS_INTERNATIONAL_BUILD || !this.mMonitor.isSupportLockScreenMagazineLeft()) {
            Button button = this.mSettingButton;
            if (this.mIsSettingButtonDarkMode) {
                i = R.drawable.lock_screen_magazine_pre_settings_dark;
            } else {
                i = R.drawable.lock_screen_magazine_pre_settings;
            }
            button.setBackgroundResource(i);
            this.mLinkButton.setBackgroundResource(R.drawable.lock_screen_magazine_pre_link);
            this.mPreviewButton.setBackgroundResource(R.drawable.lock_screen_magazine_des_more_btn_bg);
            return;
        }
        ImageView imageView = this.mGlobalLeftIcon;
        if (this.mIsGlobalPreDarkMode) {
            i2 = R.drawable.lock_screen_magazine_pre_global_left_box_dark;
        } else {
            i2 = R.drawable.lock_screen_magazine_pre_global_left_box;
        }
        imageView.setBackgroundResource(i2);
        ImageView imageView2 = this.mGlobalBottomIcon;
        if (this.mIsGlobalPreDarkMode) {
            i3 = R.drawable.lock_screen_magazine_pre_global_left_arrow_dark;
        } else {
            i3 = R.drawable.lock_screen_magazine_pre_global_left_arrow;
        }
        imageView2.setBackgroundResource(i3);
        ImageView imageView3 = this.mGlobalBottomLine;
        if (this.mIsGlobalPreDarkMode) {
            i4 = R.drawable.lock_screen_magazine_pre_global_bottom_line_dark;
        } else {
            i4 = R.drawable.lock_screen_magazine_pre_global_bottom_line;
        }
        imageView3.setBackgroundResource(i4);
    }

    private void updateViewsLayoutParams() {
        if (!miui.os.Build.IS_INTERNATIONAL_BUILD || !this.mMonitor.isSupportLockScreenMagazineLeft()) {
            LinearLayout.LayoutParams settingLayoutParams = (LinearLayout.LayoutParams) this.mSettingButton.getLayoutParams();
            settingLayoutParams.setMargins(0, this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_setting_margin_top), this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_setting_margin_end), 0);
            this.mSettingButton.setLayoutParams(settingLayoutParams);
            this.mDesLayout.setPadding(this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_des_padding_start), this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_des_padding_top), this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_des_padding_end), this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_des_padding_bottom));
            LinearLayout.LayoutParams titleLayoutParams = (LinearLayout.LayoutParams) this.mTitleLayout.getLayoutParams();
            titleLayoutParams.setMargins(0, 0, 0, this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_title_margin_bottom));
            this.mTitleLayout.setLayoutParams(titleLayoutParams);
            RelativeLayout.LayoutParams linkLayoutParams = (RelativeLayout.LayoutParams) this.mLinkButton.getLayoutParams();
            linkLayoutParams.width = this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_title_link_width_height);
            linkLayoutParams.height = this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_title_link_width_height);
            this.mLinkButton.setLayoutParams(linkLayoutParams);
            LinearLayout.LayoutParams contentLayoutParams = (LinearLayout.LayoutParams) this.mContent.getLayoutParams();
            contentLayoutParams.setMargins(0, 0, 0, this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_content_margin_bottom));
            this.mContent.setLayoutParams(contentLayoutParams);
            LinearLayout.LayoutParams buttonLayoutParams = (LinearLayout.LayoutParams) this.mPreviewButton.getLayoutParams();
            buttonLayoutParams.setMargins(this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_button_margin_start), 0, 0, 0);
            this.mPreviewButton.setLayoutParams(buttonLayoutParams);
            this.mPreviewButton.setPadding(this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_button_padding_start), this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_button_padding_top), this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_button_padding_end), this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_button_padding_bottom));
            return;
        }
        LinearLayout.LayoutParams globalLayoutParams = (LinearLayout.LayoutParams) this.mGlobalPreLayout.getLayoutParams();
        globalLayoutParams.setMargins(0, this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_des_margin_top), 0, 0);
        this.mGlobalPreLayout.setLayoutParams(globalLayoutParams);
        LinearLayout.LayoutParams globalTitleLayoutParams = (LinearLayout.LayoutParams) this.mGlobalTitle.getLayoutParams();
        globalTitleLayoutParams.setMargins(this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_title_margin_start), 0, 0, 0);
        this.mGlobalTitle.setLayoutParams(globalTitleLayoutParams);
        LinearLayout.LayoutParams globalBottomIconParams = (LinearLayout.LayoutParams) this.mGlobalBottomIcon.getLayoutParams();
        globalBottomIconParams.height = this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_bottom_icon_width);
        globalBottomIconParams.width = this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_bottom_icon_width);
        globalBottomIconParams.setMargins(this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_bottom_icon_margin_start), 0, 0, 0);
        this.mGlobalBottomIcon.setLayoutParams(globalBottomIconParams);
        LinearLayout.LayoutParams globalProviderParams = (LinearLayout.LayoutParams) this.mGlobalProvider.getLayoutParams();
        globalProviderParams.setMargins(this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_bottom_icon_margin_start), 0, 0, 0);
        this.mGlobalProvider.setLayoutParams(globalProviderParams);
        LinearLayout.LayoutParams globalBottomLineParams = (LinearLayout.LayoutParams) this.mGlobalBottomLine.getLayoutParams();
        globalBottomLineParams.setMargins(this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_bottom_line_margin), 0, this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_bottom_line_margin), 0);
        this.mGlobalBottomLine.setLayoutParams(globalBottomLineParams);
        LinearLayout.LayoutParams globalButtonLayoutParams = (LinearLayout.LayoutParams) this.mGlobalButton.getLayoutParams();
        globalButtonLayoutParams.width = this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_button_width);
        globalButtonLayoutParams.height = this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_button_height);
        globalButtonLayoutParams.setMargins(this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_button_margin_start), 0, this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_button_margin_end), 0);
        this.mGlobalButton.setLayoutParams(globalButtonLayoutParams);
    }

    public void refreshWallpaperInfo() {
        this.mLockScreenMagazineWallpaperInfo = this.mMonitor.getLockScreenMagazineWallpaperInfo();
        if (this.mLockScreenMagazineWallpaperInfo != null) {
            if (!miui.os.Build.IS_INTERNATIONAL_BUILD && !TextUtils.isEmpty(this.mLockScreenMagazineWallpaperInfo.content)) {
                this.mLockScreenMagazineWallpaperInfo.content = this.mLockScreenMagazineWallpaperInfo.content.replaceAll("\\s*", "");
            }
            updateViews();
        }
    }

    public boolean openAd() {
        return WallpaperAuthorityUtils.isLockScreenMagazineWallpaper() && this.mLockScreenMagazineWallpaperInfo != null && this.mLockScreenMagazineWallpaperInfo.opendAd(this.mContext);
    }

    public void updateViews() {
        if (!miui.os.Build.IS_INTERNATIONAL_BUILD || !this.mMonitor.isSupportLockScreenMagazineLeft()) {
            updateTitle();
            updateContentText();
            updatePreviewButtonText();
            return;
        }
        updateGlobalTitle();
        updateGlobalLeftIcon();
        updateGlobalProviderText();
        updateGlobalSourceText();
        updateGlobalButton();
    }

    private void updatePreviewButtonText() {
        String text;
        if (this.mLockScreenMagazineWallpaperInfo != null) {
            if (!this.mMonitor.isLockScreenMagazinePkgExist()) {
                text = this.mContext.getResources().getString(R.string.download_lock_wallpaper);
            } else if (WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper()) {
                text = this.mContext.getResources().getString(R.string.lock_wallpaper_enter);
            } else if (!TextUtils.isEmpty(this.mLockScreenMagazineWallpaperInfo.btnText)) {
                text = this.mLockScreenMagazineWallpaperInfo.btnText;
            } else {
                text = this.mContent.getResources().getString(R.string.custom_lock_wallpaper_enter);
            }
            if (!TextUtils.isEmpty(text)) {
                this.mPreviewButton.setText(text);
            }
        }
    }

    private void updateTitle() {
        String titleRes = getTitleText();
        if (!TextUtils.isEmpty(titleRes)) {
            this.mTitle.setVisibility(0);
            this.mTitle.setText(titleRes);
            updateLinkButton();
            return;
        }
        this.mTitle.setVisibility(8);
    }

    /* access modifiers changed from: private */
    public void updateLinkButton() {
        if (this.mLockScreenMagazineWallpaperInfo == null || TextUtils.isEmpty(this.mLockScreenMagazineWallpaperInfo.landingPageUrl)) {
            updateLinkButtonLayoutParams((int) this.mTitleLayoutWidth, 0, 0);
            this.mLinkButton.setVisibility(8);
            return;
        }
        float linkWidth = (float) this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_title_link_width_height);
        float linkMarginStart = (float) this.mResources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_title_link_margin_start);
        int titleTextWidth = getTitleTextWidth();
        float linkPlaceWidth = linkWidth + linkMarginStart;
        if (((float) (titleTextWidth + 20)) > this.mTitleLayoutWidth - linkPlaceWidth) {
            updateLinkButtonLayoutParams((int) (this.mTitleLayoutWidth - linkPlaceWidth), (int) linkPlaceWidth, (int) (this.mTitleLayoutWidth - linkWidth));
        } else {
            updateLinkButtonLayoutParams(titleTextWidth + 20, 0, (int) (((float) (titleTextWidth + 20)) + linkMarginStart));
        }
        this.mLinkButton.setVisibility(0);
        this.mLinkButton.setOnClickListener(this.mLinkClickListener);
        this.mLinkButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                return true;
            }
        });
        if (this.mLockScreenMagazineWallpaperInfo.linkType == 1) {
            this.mLinkButton.setBackgroundResource(R.drawable.lock_screen_magazine_pre_link);
        } else if (this.mLockScreenMagazineWallpaperInfo.linkType == 2) {
            this.mLinkButton.setBackgroundResource(R.drawable.wallpaper_play);
        }
    }

    private int getTitleTextWidth() {
        String text = this.mTitle.getText().toString();
        Rect bounds = new Rect();
        this.mTitle.getPaint().getTextBounds(text, 0, text.length(), bounds);
        return bounds.width();
    }

    private void updateLinkButtonLayoutParams(int titleWidth, int titleRightMargin, int linkLeftMargin) {
        RelativeLayout.LayoutParams titleLayoutParams = (RelativeLayout.LayoutParams) this.mTitle.getLayoutParams();
        titleLayoutParams.width = titleWidth;
        titleLayoutParams.rightMargin = titleRightMargin;
        this.mTitle.setLayoutParams(titleLayoutParams);
        RelativeLayout.LayoutParams linkLayoutParams = (RelativeLayout.LayoutParams) this.mLinkButton.getLayoutParams();
        linkLayoutParams.leftMargin = linkLeftMargin;
        this.mLinkButton.setLayoutParams(linkLayoutParams);
    }

    private void updateGlobalLeftIcon() {
        this.mGlobalLeftIcon.setVisibility(TextUtils.isEmpty(getGlobalTitleText()) ? 8 : 0);
    }

    private void updateGlobalTitle() {
        String titleRes = getGlobalTitleText();
        if (!TextUtils.isEmpty(titleRes)) {
            this.mGlobalTitle.setVisibility(0);
            this.mGlobalTitle.setText(titleRes);
            return;
        }
        this.mGlobalTitle.setVisibility(8);
    }

    private String getTitleText() {
        if (!WallpaperAuthorityUtils.isLockScreenMagazineWallpaper()) {
            return null;
        }
        String titleRes = getResources().getString(R.string.lock_screen_magazine_default_title);
        if (this.mLockScreenMagazineWallpaperInfo == null) {
            return titleRes;
        }
        if (this.mLockScreenMagazineWallpaperInfo.isTitleCustomized) {
            if (!TextUtils.isEmpty(this.mLockScreenMagazineWallpaperInfo.title)) {
                titleRes = this.mLockScreenMagazineWallpaperInfo.title;
            }
        } else if (!TextUtils.isEmpty(this.mLockScreenMagazineWallpaperInfo.title) && (miui.os.Build.IS_INTERNATIONAL_BUILD || Locale.CHINESE.getLanguage().equals(Locale.getDefault().getLanguage()))) {
            titleRes = this.mLockScreenMagazineWallpaperInfo.title;
        }
        return titleRes;
    }

    private String getGlobalTitleText() {
        if (!WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper() || this.mLockScreenMagazineWallpaperInfo == null) {
            return null;
        }
        String titleRes = null;
        if (!TextUtils.isEmpty(this.mLockScreenMagazineWallpaperInfo.title)) {
            titleRes = this.mLockScreenMagazineWallpaperInfo.title;
        }
        return titleRes;
    }

    private void updateContentText() {
        String contentRes = getContentText();
        if (!TextUtils.isEmpty(contentRes)) {
            this.mContent.setVisibility(0);
            this.mContent.setText(contentRes);
            return;
        }
        this.mContent.setVisibility(8);
    }

    private void updateGlobalProviderText() {
        String res = getGlobalProviderText();
        if (TextUtils.isEmpty(res) || TextUtils.isEmpty(getGlobalTitleText())) {
            this.mGlobalProvider.setVisibility(8);
            return;
        }
        this.mGlobalProvider.setVisibility(0);
        this.mGlobalProvider.setText(res);
    }

    private void updateGlobalSourceText() {
        String res = getGlobalSourceText();
        if (TextUtils.isEmpty(res) || TextUtils.isEmpty(getGlobalTitleText())) {
            this.mGlobalSource.setVisibility(8);
            this.mGlobalBottomLine.setVisibility(8);
            return;
        }
        this.mGlobalSource.setVisibility(0);
        this.mGlobalSource.setText(res);
        if (this.mGlobalProvider.getVisibility() == 8) {
            this.mGlobalBottomLine.setVisibility(8);
        } else {
            this.mGlobalBottomLine.setVisibility(0);
        }
    }

    private void updateGlobalButton() {
        this.mGlobalButton.setVisibility(!TextUtils.isEmpty(getGlobalTitleText()) ? 0 : 8);
        this.mGlobalButton.setText(R.string.lock_screen_magazine_global_button_text);
    }

    private String getContentText() {
        if (!WallpaperAuthorityUtils.isLockScreenMagazineWallpaper()) {
            return null;
        }
        String contentRes = getResources().getString(R.string.lock_screen_magazine_default_content);
        if (this.mLockScreenMagazineWallpaperInfo != null && !TextUtils.isEmpty(this.mLockScreenMagazineWallpaperInfo.content) && (miui.os.Build.IS_INTERNATIONAL_BUILD || Locale.CHINESE.getLanguage().equals(Locale.getDefault().getLanguage()))) {
            contentRes = this.mLockScreenMagazineWallpaperInfo.content;
        }
        return contentRes;
    }

    private String getGlobalProviderText() {
        if (!WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper()) {
            return null;
        }
        String res = null;
        if (this.mLockScreenMagazineWallpaperInfo != null && !TextUtils.isEmpty(this.mLockScreenMagazineWallpaperInfo.provider)) {
            res = this.mLockScreenMagazineWallpaperInfo.provider;
        }
        return res;
    }

    private String getGlobalSourceText() {
        if (!WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper()) {
            return null;
        }
        String res = null;
        if (this.mLockScreenMagazineWallpaperInfo != null && !TextUtils.isEmpty(this.mLockScreenMagazineWallpaperInfo.source)) {
            res = this.mLockScreenMagazineWallpaperInfo.source;
        }
        return res;
    }

    public void setSettingButtonDarkMode(boolean isDarkMode) {
        int i;
        this.mIsSettingButtonDarkMode = isDarkMode;
        Button button = this.mSettingButton;
        if (isDarkMode) {
            i = R.drawable.lock_screen_magazine_pre_settings_dark;
        } else {
            i = R.drawable.lock_screen_magazine_pre_settings;
        }
        button.setBackgroundResource(i);
    }

    public void setGlobalPreDarkMode(boolean isDarkMode) {
        int i;
        int i2;
        int i3;
        int i4;
        this.mIsGlobalPreDarkMode = isDarkMode;
        int color = this.mIsGlobalPreDarkMode ? getContext().getResources().getColor(R.color.miui_common_unlock_screen_common_time_dark_text_color) : -1;
        this.mGlobalTitle.setTextColor(color);
        this.mGlobalProvider.setTextColor(color);
        this.mGlobalSource.setTextColor(color);
        this.mGlobalButton.setTextColor(color);
        ImageView imageView = this.mGlobalLeftIcon;
        if (this.mIsGlobalPreDarkMode) {
            i = R.drawable.lock_screen_magazine_pre_global_left_box_dark;
        } else {
            i = R.drawable.lock_screen_magazine_pre_global_left_box;
        }
        imageView.setBackgroundResource(i);
        ImageView imageView2 = this.mGlobalBottomIcon;
        if (this.mIsGlobalPreDarkMode) {
            i2 = R.drawable.lock_screen_magazine_pre_global_left_arrow_dark;
        } else {
            i2 = R.drawable.lock_screen_magazine_pre_global_left_arrow;
        }
        imageView2.setBackgroundResource(i2);
        ImageView imageView3 = this.mGlobalBottomLine;
        if (this.mIsGlobalPreDarkMode) {
            i3 = R.drawable.lock_screen_magazine_pre_global_bottom_line_dark;
        } else {
            i3 = R.drawable.lock_screen_magazine_pre_global_bottom_line;
        }
        imageView3.setBackgroundResource(i3);
        TextView textView = this.mGlobalButton;
        if (this.mIsGlobalPreDarkMode) {
            i4 = R.drawable.lock_screen_magazine_des_global_more_btn_bg_dark;
        } else {
            i4 = R.drawable.lock_screen_magazine_des_global_more_btn_bg;
        }
        textView.setBackgroundResource(i4);
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }
}
