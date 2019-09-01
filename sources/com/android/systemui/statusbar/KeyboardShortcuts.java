package com.android.systemui.statusbar;

import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyboardShortcutGroup;
import android.view.KeyboardShortcutInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManagerCompat;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.internal.app.AssistUtils;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.util.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class KeyboardShortcuts {
    private static final String TAG = KeyboardShortcuts.class.getSimpleName();
    private static KeyboardShortcuts sInstance;
    private static final Object sLock = new Object();
    private final Comparator<KeyboardShortcutInfo> mApplicationItemsComparator = new Comparator<KeyboardShortcutInfo>() {
        public int compare(KeyboardShortcutInfo ksh1, KeyboardShortcutInfo ksh2) {
            boolean ksh1ShouldBeLast = ksh1.getLabel() == null || ksh1.getLabel().toString().isEmpty();
            boolean ksh2ShouldBeLast = ksh2.getLabel() == null || ksh2.getLabel().toString().isEmpty();
            if (ksh1ShouldBeLast && ksh2ShouldBeLast) {
                return 0;
            }
            if (ksh1ShouldBeLast) {
                return 1;
            }
            if (ksh2ShouldBeLast) {
                return -1;
            }
            return ksh1.getLabel().toString().compareToIgnoreCase(ksh2.getLabel().toString());
        }
    };
    private KeyCharacterMap mBackupKeyCharacterMap;
    private final Context mContext;
    private final DialogInterface.OnClickListener mDialogCloseListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            KeyboardShortcuts.this.dismissKeyboardShortcuts();
        }
    };
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private KeyCharacterMap mKeyCharacterMap;
    private Dialog mKeyboardShortcutsDialog;
    private final SparseArray<Drawable> mModifierDrawables = new SparseArray<>();
    private final int[] mModifierList = {65536, 4096, 2, 1, 4, 8};
    private final SparseArray<String> mModifierNames = new SparseArray<>();
    private final IPackageManager mPackageManager;
    private final SparseArray<Drawable> mSpecialCharacterDrawables = new SparseArray<>();
    private final SparseArray<String> mSpecialCharacterNames = new SparseArray<>();

    private final class ShortcutKeyAccessibilityDelegate extends View.AccessibilityDelegate {
        private String mContentDescription;

        ShortcutKeyAccessibilityDelegate(String contentDescription) {
            this.mContentDescription = contentDescription;
        }

        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            if (this.mContentDescription != null) {
                info.setContentDescription(this.mContentDescription.toLowerCase());
            }
        }
    }

    private static final class StringDrawableContainer {
        public Drawable mDrawable;
        public String mString;

        StringDrawableContainer(String string, Drawable drawable) {
            this.mString = string;
            this.mDrawable = drawable;
        }
    }

    private KeyboardShortcuts(Context context) {
        this.mContext = new ContextThemeWrapper(context, 16974123);
        this.mPackageManager = AppGlobals.getPackageManager();
        loadResources(context);
    }

    private static KeyboardShortcuts getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new KeyboardShortcuts(context);
        }
        return sInstance;
    }

    public static void show(Context context, int deviceId) {
        MetricsLogger.visible(context, 500);
        synchronized (sLock) {
            if (sInstance != null && !sInstance.mContext.equals(context)) {
                dismiss();
            }
            getInstance(context).showKeyboardShortcuts(deviceId);
        }
    }

    public static void toggle(Context context, int deviceId) {
        synchronized (sLock) {
            if (isShowing()) {
                dismiss();
            } else {
                show(context, deviceId);
            }
        }
    }

    public static void dismiss() {
        synchronized (sLock) {
            if (sInstance != null) {
                MetricsLogger.hidden(sInstance.mContext, 500);
                sInstance.dismissKeyboardShortcuts();
                sInstance = null;
            }
        }
    }

    private static boolean isShowing() {
        return (sInstance == null || sInstance.mKeyboardShortcutsDialog == null || !sInstance.mKeyboardShortcutsDialog.isShowing()) ? false : true;
    }

    private void loadResources(Context context) {
        this.mSpecialCharacterNames.put(3, context.getString(R.string.keyboard_key_home));
        this.mSpecialCharacterNames.put(4, context.getString(R.string.keyboard_key_back));
        this.mSpecialCharacterNames.put(19, context.getString(R.string.keyboard_key_dpad_up));
        this.mSpecialCharacterNames.put(20, context.getString(R.string.keyboard_key_dpad_down));
        this.mSpecialCharacterNames.put(21, context.getString(R.string.keyboard_key_dpad_left));
        this.mSpecialCharacterNames.put(22, context.getString(R.string.keyboard_key_dpad_right));
        this.mSpecialCharacterNames.put(23, context.getString(R.string.keyboard_key_dpad_center));
        this.mSpecialCharacterNames.put(56, ".");
        this.mSpecialCharacterNames.put(61, context.getString(R.string.keyboard_key_tab));
        this.mSpecialCharacterNames.put(62, context.getString(R.string.keyboard_key_space));
        this.mSpecialCharacterNames.put(66, context.getString(R.string.keyboard_key_enter));
        this.mSpecialCharacterNames.put(67, context.getString(R.string.keyboard_key_backspace));
        this.mSpecialCharacterNames.put(85, context.getString(R.string.keyboard_key_media_play_pause));
        this.mSpecialCharacterNames.put(86, context.getString(R.string.keyboard_key_media_stop));
        this.mSpecialCharacterNames.put(87, context.getString(R.string.keyboard_key_media_next));
        this.mSpecialCharacterNames.put(88, context.getString(R.string.keyboard_key_media_previous));
        this.mSpecialCharacterNames.put(89, context.getString(R.string.keyboard_key_media_rewind));
        this.mSpecialCharacterNames.put(90, context.getString(R.string.keyboard_key_media_fast_forward));
        this.mSpecialCharacterNames.put(92, context.getString(R.string.keyboard_key_page_up));
        this.mSpecialCharacterNames.put(93, context.getString(R.string.keyboard_key_page_down));
        this.mSpecialCharacterNames.put(96, context.getString(R.string.keyboard_key_button_template, new Object[]{"A"}));
        this.mSpecialCharacterNames.put(97, context.getString(R.string.keyboard_key_button_template, new Object[]{"B"}));
        this.mSpecialCharacterNames.put(98, context.getString(R.string.keyboard_key_button_template, new Object[]{"C"}));
        this.mSpecialCharacterNames.put(99, context.getString(R.string.keyboard_key_button_template, new Object[]{"X"}));
        this.mSpecialCharacterNames.put(100, context.getString(R.string.keyboard_key_button_template, new Object[]{"Y"}));
        this.mSpecialCharacterNames.put(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle, context.getString(R.string.keyboard_key_button_template, new Object[]{"Z"}));
        this.mSpecialCharacterNames.put(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSearchResultTitle, context.getString(R.string.keyboard_key_button_template, new Object[]{"L1"}));
        this.mSpecialCharacterNames.put(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSmallPopupMenu, context.getString(R.string.keyboard_key_button_template, new Object[]{"R1"}));
        this.mSpecialCharacterNames.put(com.android.systemui.plugins.R.styleable.AppCompatTheme_textColorAlertDialogListItem, context.getString(R.string.keyboard_key_button_template, new Object[]{"L2"}));
        this.mSpecialCharacterNames.put(com.android.systemui.plugins.R.styleable.AppCompatTheme_textColorSearchUrl, context.getString(R.string.keyboard_key_button_template, new Object[]{"R2"}));
        this.mSpecialCharacterNames.put(com.android.systemui.plugins.R.styleable.AppCompatTheme_tooltipForegroundColor, context.getString(R.string.keyboard_key_button_template, new Object[]{"Start"}));
        this.mSpecialCharacterNames.put(com.android.systemui.plugins.R.styleable.AppCompatTheme_tooltipFrameBackground, context.getString(R.string.keyboard_key_button_template, new Object[]{"Select"}));
        this.mSpecialCharacterNames.put(110, context.getString(R.string.keyboard_key_button_template, new Object[]{"Mode"}));
        this.mSpecialCharacterNames.put(112, context.getString(R.string.keyboard_key_forward_del));
        this.mSpecialCharacterNames.put(111, "Esc");
        this.mSpecialCharacterNames.put(120, "SysRq");
        this.mSpecialCharacterNames.put(121, "Break");
        this.mSpecialCharacterNames.put(116, "Scroll Lock");
        this.mSpecialCharacterNames.put(122, context.getString(R.string.keyboard_key_move_home));
        this.mSpecialCharacterNames.put(123, context.getString(R.string.keyboard_key_move_end));
        this.mSpecialCharacterNames.put(124, context.getString(R.string.keyboard_key_insert));
        this.mSpecialCharacterNames.put(131, "F1");
        this.mSpecialCharacterNames.put(132, "F2");
        this.mSpecialCharacterNames.put(133, "F3");
        this.mSpecialCharacterNames.put(134, "F4");
        this.mSpecialCharacterNames.put(135, "F5");
        this.mSpecialCharacterNames.put(136, "F6");
        this.mSpecialCharacterNames.put(137, "F7");
        this.mSpecialCharacterNames.put(138, "F8");
        this.mSpecialCharacterNames.put(139, "F9");
        this.mSpecialCharacterNames.put(140, "F10");
        this.mSpecialCharacterNames.put(141, "F11");
        this.mSpecialCharacterNames.put(142, "F12");
        this.mSpecialCharacterNames.put(143, context.getString(R.string.keyboard_key_num_lock));
        this.mSpecialCharacterNames.put(144, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"0"}));
        this.mSpecialCharacterNames.put(145, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"1"}));
        this.mSpecialCharacterNames.put(146, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"2"}));
        this.mSpecialCharacterNames.put(147, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"3"}));
        this.mSpecialCharacterNames.put(148, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"4"}));
        this.mSpecialCharacterNames.put(149, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"5"}));
        this.mSpecialCharacterNames.put(150, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"6"}));
        this.mSpecialCharacterNames.put(151, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"7"}));
        this.mSpecialCharacterNames.put(152, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"8"}));
        this.mSpecialCharacterNames.put(153, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"9"}));
        this.mSpecialCharacterNames.put(154, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"/"}));
        this.mSpecialCharacterNames.put(155, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"*"}));
        this.mSpecialCharacterNames.put(156, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"-"}));
        this.mSpecialCharacterNames.put(157, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"+"}));
        this.mSpecialCharacterNames.put(158, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"."}));
        this.mSpecialCharacterNames.put(159, context.getString(R.string.keyboard_key_numpad_template, new Object[]{","}));
        this.mSpecialCharacterNames.put(160, context.getString(R.string.keyboard_key_numpad_template, new Object[]{context.getString(R.string.keyboard_key_enter)}));
        this.mSpecialCharacterNames.put(161, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"="}));
        this.mSpecialCharacterNames.put(162, context.getString(R.string.keyboard_key_numpad_template, new Object[]{"("}));
        this.mSpecialCharacterNames.put(163, context.getString(R.string.keyboard_key_numpad_template, new Object[]{")"}));
        this.mSpecialCharacterNames.put(211, "半角/全角");
        this.mSpecialCharacterNames.put(212, "英数");
        this.mSpecialCharacterNames.put(213, "無変換");
        this.mSpecialCharacterNames.put(214, "変換");
        this.mSpecialCharacterNames.put(215, "かな");
        this.mModifierNames.put(65536, "Meta");
        this.mModifierNames.put(4096, "Ctrl");
        this.mModifierNames.put(2, "Alt");
        this.mModifierNames.put(1, "Shift");
        this.mModifierNames.put(4, "Sym");
        this.mModifierNames.put(8, "Fn");
        this.mSpecialCharacterDrawables.put(67, context.getDrawable(R.drawable.ic_ksh_key_backspace));
        this.mSpecialCharacterDrawables.put(66, context.getDrawable(R.drawable.ic_ksh_key_enter));
        this.mSpecialCharacterDrawables.put(19, context.getDrawable(R.drawable.ic_ksh_key_up));
        this.mSpecialCharacterDrawables.put(22, context.getDrawable(R.drawable.ic_ksh_key_right));
        this.mSpecialCharacterDrawables.put(20, context.getDrawable(R.drawable.ic_ksh_key_down));
        this.mSpecialCharacterDrawables.put(21, context.getDrawable(R.drawable.ic_ksh_key_left));
        this.mModifierDrawables.put(65536, context.getDrawable(R.drawable.ic_ksh_key_meta));
    }

    private void retrieveKeyCharacterMap(int deviceId) {
        InputManager inputManager = InputManager.getInstance();
        this.mBackupKeyCharacterMap = inputManager.getInputDevice(-1).getKeyCharacterMap();
        if (deviceId != -1) {
            InputDevice inputDevice = inputManager.getInputDevice(deviceId);
            if (inputDevice != null) {
                this.mKeyCharacterMap = inputDevice.getKeyCharacterMap();
                return;
            }
        }
        int[] deviceIds = inputManager.getInputDeviceIds();
        int i = 0;
        while (i < deviceIds.length) {
            InputDevice inputDevice2 = inputManager.getInputDevice(deviceIds[i]);
            if (inputDevice2.getId() == -1 || !inputDevice2.isFullKeyboard()) {
                i++;
            } else {
                this.mKeyCharacterMap = inputDevice2.getKeyCharacterMap();
                return;
            }
        }
        this.mKeyCharacterMap = this.mBackupKeyCharacterMap;
    }

    private void showKeyboardShortcuts(int deviceId) {
        retrieveKeyCharacterMap(deviceId);
        WindowManagerCompat.requestAppKeyboardShortcuts(Recents.getSystemServices().mWm, new WindowManagerCompat.IKeyboardShortcutsReceiver() {
            public void onKeyboardShortcutsReceived(List<KeyboardShortcutGroup> result) {
                result.add(KeyboardShortcuts.this.getSystemShortcuts());
                KeyboardShortcutGroup appShortcuts = KeyboardShortcuts.this.getDefaultApplicationShortcuts();
                if (appShortcuts != null) {
                    result.add(appShortcuts);
                }
                KeyboardShortcuts.this.showKeyboardShortcutsDialog(result);
            }
        }, deviceId);
    }

    /* access modifiers changed from: private */
    public void dismissKeyboardShortcuts() {
        if (this.mKeyboardShortcutsDialog != null) {
            this.mKeyboardShortcutsDialog.dismiss();
            this.mKeyboardShortcutsDialog = null;
        }
    }

    /* access modifiers changed from: private */
    public KeyboardShortcutGroup getSystemShortcuts() {
        KeyboardShortcutGroup systemGroup = new KeyboardShortcutGroup(this.mContext.getString(R.string.keyboard_shortcut_group_system), true);
        systemGroup.addItem(new KeyboardShortcutInfo(this.mContext.getString(R.string.keyboard_shortcut_group_system_home), 66, 65536));
        systemGroup.addItem(new KeyboardShortcutInfo(this.mContext.getString(R.string.keyboard_shortcut_group_system_back), 67, 65536));
        systemGroup.addItem(new KeyboardShortcutInfo(this.mContext.getString(R.string.keyboard_shortcut_group_system_recents), 61, 2));
        systemGroup.addItem(new KeyboardShortcutInfo(this.mContext.getString(R.string.keyboard_shortcut_group_system_notifications), 42, 65536));
        systemGroup.addItem(new KeyboardShortcutInfo(this.mContext.getString(R.string.keyboard_shortcut_group_system_shortcuts_helper), 76, 65536));
        systemGroup.addItem(new KeyboardShortcutInfo(this.mContext.getString(R.string.keyboard_shortcut_group_system_switch_input), 62, 65536));
        return systemGroup;
    }

    /* access modifiers changed from: private */
    public KeyboardShortcutGroup getDefaultApplicationShortcuts() {
        int userId = this.mContext.getUserId();
        List<KeyboardShortcutInfo> keyboardShortcutInfoAppItems = new ArrayList<>();
        ComponentName assistComponent = new AssistUtils(this.mContext).getAssistComponentForUser(userId);
        if (assistComponent != null) {
            PackageInfo assistPackageInfo = null;
            try {
                assistPackageInfo = this.mPackageManager.getPackageInfo(assistComponent.getPackageName(), 0, userId);
            } catch (RemoteException e) {
                Log.e(TAG, "PackageManagerService is dead");
            }
            if (assistPackageInfo != null) {
                keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(R.string.keyboard_shortcut_group_applications_assist), Icon.createWithResource(assistPackageInfo.applicationInfo.packageName, assistPackageInfo.applicationInfo.icon), 0, 65536));
            }
        }
        Icon browserIcon = getIconForIntentCategory("android.intent.category.APP_BROWSER", userId);
        if (browserIcon != null) {
            keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(R.string.keyboard_shortcut_group_applications_browser), browserIcon, 30, 65536));
        }
        Icon contactsIcon = getIconForIntentCategory("android.intent.category.APP_CONTACTS", userId);
        if (contactsIcon != null) {
            keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(R.string.keyboard_shortcut_group_applications_contacts), contactsIcon, 31, 65536));
        }
        Icon emailIcon = getIconForIntentCategory("android.intent.category.APP_EMAIL", userId);
        if (emailIcon != null) {
            keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(R.string.keyboard_shortcut_group_applications_email), emailIcon, 33, 65536));
        }
        Icon messagingIcon = getIconForIntentCategory("android.intent.category.APP_MESSAGING", userId);
        if (messagingIcon != null) {
            keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(R.string.keyboard_shortcut_group_applications_sms), messagingIcon, 47, 65536));
        }
        Icon musicIcon = getIconForIntentCategory("android.intent.category.APP_MUSIC", userId);
        if (musicIcon != null) {
            keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(R.string.keyboard_shortcut_group_applications_music), musicIcon, 44, 65536));
        }
        Icon calendarIcon = getIconForIntentCategory("android.intent.category.APP_CALENDAR", userId);
        if (calendarIcon != null) {
            keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(R.string.keyboard_shortcut_group_applications_calendar), calendarIcon, 40, 65536));
        }
        if (keyboardShortcutInfoAppItems.size() == 0) {
            return null;
        }
        Collections.sort(keyboardShortcutInfoAppItems, this.mApplicationItemsComparator);
        return new KeyboardShortcutGroup(this.mContext.getString(R.string.keyboard_shortcut_group_applications), keyboardShortcutInfoAppItems, true);
    }

    private Icon getIconForIntentCategory(String intentCategory, int userId) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory(intentCategory);
        PackageInfo packageInfo = getPackageInfoForIntent(intent, userId);
        if (packageInfo == null || packageInfo.applicationInfo.icon == 0) {
            return null;
        }
        return Icon.createWithResource(packageInfo.applicationInfo.packageName, packageInfo.applicationInfo.icon);
    }

    private PackageInfo getPackageInfoForIntent(Intent intent, int userId) {
        try {
            ResolveInfo handler = this.mPackageManager.resolveIntent(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 0, userId);
            if (handler != null) {
                if (handler.activityInfo != null) {
                    return this.mPackageManager.getPackageInfo(handler.activityInfo.packageName, 0, userId);
                }
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "PackageManagerService is dead", e);
            return null;
        }
    }

    /* access modifiers changed from: private */
    public void showKeyboardShortcutsDialog(final List<KeyboardShortcutGroup> keyboardShortcutGroups) {
        this.mHandler.post(new Runnable() {
            public void run() {
                KeyboardShortcuts.this.handleShowKeyboardShortcuts(keyboardShortcutGroups);
            }
        });
    }

    /* access modifiers changed from: private */
    public void handleShowKeyboardShortcuts(List<KeyboardShortcutGroup> keyboardShortcutGroups) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this.mContext);
        View keyboardShortcutsView = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(R.layout.keyboard_shortcuts_view, null);
        populateKeyboardShortcuts((LinearLayout) keyboardShortcutsView.findViewById(R.id.keyboard_shortcuts_container), keyboardShortcutGroups);
        dialogBuilder.setView(keyboardShortcutsView);
        dialogBuilder.setPositiveButton(R.string.quick_settings_done, this.mDialogCloseListener);
        this.mKeyboardShortcutsDialog = dialogBuilder.create();
        this.mKeyboardShortcutsDialog.setCanceledOnTouchOutside(true);
        this.mKeyboardShortcutsDialog.getWindow().setType(2008);
        synchronized (sLock) {
            if (sInstance != null) {
                this.mKeyboardShortcutsDialog.show();
            }
        }
    }

    private void populateKeyboardShortcuts(LinearLayout keyboardShortcutsLayout, List<KeyboardShortcutGroup> keyboardShortcutGroups) {
        int i;
        boolean z;
        int keyboardShortcutGroupsSize;
        int i2;
        int itemsSize;
        int shortcutKeyIconItemHeightWidth;
        TextView categoryTitle;
        KeyboardShortcutGroup group;
        TextView shortcutsKeyView;
        int keyboardShortcutGroupsSize2;
        int keyboardShortcutGroupsSize3;
        int i3;
        int itemsSize2;
        int shortcutKeyIconItemHeightWidth2;
        TextView categoryTitle2;
        LinearLayout linearLayout = keyboardShortcutsLayout;
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        int keyboardShortcutGroupsSize4 = keyboardShortcutGroups.size();
        int i4 = R.layout.keyboard_shortcuts_key_view;
        boolean z2 = false;
        TextView shortcutsKeyView2 = (TextView) inflater.inflate(R.layout.keyboard_shortcuts_key_view, null, false);
        shortcutsKeyView2.measure(0, 0);
        int shortcutKeyTextItemMinWidth = shortcutsKeyView2.getMeasuredHeight();
        int shortcutKeyIconItemHeightWidth3 = (shortcutsKeyView2.getMeasuredHeight() - shortcutsKeyView2.getPaddingTop()) - shortcutsKeyView2.getPaddingBottom();
        int i5 = 0;
        while (i5 < keyboardShortcutGroupsSize4) {
            KeyboardShortcutGroup group2 = keyboardShortcutGroups.get(i5);
            TextView categoryTitle3 = (TextView) inflater.inflate(R.layout.keyboard_shortcuts_category_title, linearLayout, z2);
            categoryTitle3.setText(group2.getLabel());
            if (group2.isSystemGroup()) {
                i = Utils.getColorAccent(this.mContext);
            } else {
                i = this.mContext.getColor(R.color.ksh_application_group_color);
            }
            categoryTitle3.setTextColor(i);
            linearLayout.addView(categoryTitle3);
            LinearLayout shortcutContainer = (LinearLayout) inflater.inflate(R.layout.keyboard_shortcuts_container, linearLayout, z2);
            int itemsSize3 = group2.getItems().size();
            int j = z2;
            while (j < itemsSize3) {
                KeyboardShortcutInfo info = group2.getItems().get(j);
                List<StringDrawableContainer> shortcutKeys = getHumanReadableShortcutKeys(info);
                if (shortcutKeys == null) {
                    shortcutsKeyView = shortcutsKeyView2;
                    Log.w(TAG, "Keyboard Shortcut contains unsupported keys, skipping.");
                    keyboardShortcutGroupsSize = keyboardShortcutGroupsSize4;
                    shortcutKeyIconItemHeightWidth = shortcutKeyIconItemHeightWidth3;
                    i2 = i5;
                    group = group2;
                    categoryTitle = categoryTitle3;
                    itemsSize = itemsSize3;
                    keyboardShortcutGroupsSize2 = R.layout.keyboard_shortcuts_key_view;
                } else {
                    shortcutsKeyView = shortcutsKeyView2;
                    View shortcutView = inflater.inflate(R.layout.keyboard_shortcut_app_item, shortcutContainer, false);
                    if (info.getIcon() != null) {
                        ImageView shortcutIcon = (ImageView) shortcutView.findViewById(R.id.keyboard_shortcuts_icon);
                        group = group2;
                        shortcutIcon.setImageIcon(info.getIcon());
                        shortcutIcon.setVisibility(0);
                    } else {
                        group = group2;
                    }
                    TextView shortcutKeyword = (TextView) shortcutView.findViewById(R.id.keyboard_shortcuts_keyword);
                    shortcutKeyword.setText(info.getLabel());
                    if (info.getIcon() != null) {
                        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) shortcutKeyword.getLayoutParams();
                        KeyboardShortcutInfo keyboardShortcutInfo = info;
                        lp.removeRule(20);
                        shortcutKeyword.setLayoutParams(lp);
                    }
                    ViewGroup shortcutItemsContainer = (ViewGroup) shortcutView.findViewById(R.id.keyboard_shortcuts_item_container);
                    int shortcutKeysSize = shortcutKeys.size();
                    int k = 0;
                    while (true) {
                        TextView shortcutKeyword2 = shortcutKeyword;
                        int k2 = k;
                        if (k2 >= shortcutKeysSize) {
                            break;
                        }
                        List<StringDrawableContainer> shortcutKeys2 = shortcutKeys;
                        StringDrawableContainer shortcutRepresentation = shortcutKeys.get(k2);
                        int shortcutKeysSize2 = shortcutKeysSize;
                        if (shortcutRepresentation.mDrawable != null) {
                            categoryTitle2 = categoryTitle3;
                            ImageView shortcutKeyIconView = (ImageView) inflater.inflate(R.layout.keyboard_shortcuts_key_icon_view, shortcutItemsContainer, false);
                            Bitmap bitmap = Bitmap.createBitmap(shortcutKeyIconItemHeightWidth3, shortcutKeyIconItemHeightWidth3, Bitmap.Config.ARGB_8888);
                            shortcutKeyIconItemHeightWidth2 = shortcutKeyIconItemHeightWidth3;
                            Canvas canvas = new Canvas(bitmap);
                            itemsSize2 = itemsSize3;
                            i3 = i5;
                            keyboardShortcutGroupsSize3 = keyboardShortcutGroupsSize4;
                            shortcutRepresentation.mDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                            shortcutRepresentation.mDrawable.draw(canvas);
                            shortcutKeyIconView.setImageBitmap(bitmap);
                            shortcutKeyIconView.setImportantForAccessibility(1);
                            shortcutKeyIconView.setAccessibilityDelegate(new ShortcutKeyAccessibilityDelegate(shortcutRepresentation.mString));
                            shortcutItemsContainer.addView(shortcutKeyIconView);
                        } else {
                            keyboardShortcutGroupsSize3 = keyboardShortcutGroupsSize4;
                            shortcutKeyIconItemHeightWidth2 = shortcutKeyIconItemHeightWidth3;
                            i3 = i5;
                            categoryTitle2 = categoryTitle3;
                            itemsSize2 = itemsSize3;
                            if (shortcutRepresentation.mString != null) {
                                TextView shortcutKeyTextView = (TextView) inflater.inflate(R.layout.keyboard_shortcuts_key_view, shortcutItemsContainer, false);
                                shortcutKeyTextView.setMinimumWidth(shortcutKeyTextItemMinWidth);
                                shortcutKeyTextView.setText(shortcutRepresentation.mString);
                                shortcutKeyTextView.setAccessibilityDelegate(new ShortcutKeyAccessibilityDelegate(shortcutRepresentation.mString));
                                shortcutItemsContainer.addView(shortcutKeyTextView);
                                k = k2 + 1;
                                shortcutKeyword = shortcutKeyword2;
                                shortcutKeys = shortcutKeys2;
                                shortcutKeysSize = shortcutKeysSize2;
                                categoryTitle3 = categoryTitle2;
                                shortcutKeyIconItemHeightWidth3 = shortcutKeyIconItemHeightWidth2;
                                itemsSize3 = itemsSize2;
                                i5 = i3;
                                keyboardShortcutGroupsSize4 = keyboardShortcutGroupsSize3;
                                LinearLayout linearLayout2 = keyboardShortcutsLayout;
                            }
                        }
                        k = k2 + 1;
                        shortcutKeyword = shortcutKeyword2;
                        shortcutKeys = shortcutKeys2;
                        shortcutKeysSize = shortcutKeysSize2;
                        categoryTitle3 = categoryTitle2;
                        shortcutKeyIconItemHeightWidth3 = shortcutKeyIconItemHeightWidth2;
                        itemsSize3 = itemsSize2;
                        i5 = i3;
                        keyboardShortcutGroupsSize4 = keyboardShortcutGroupsSize3;
                        LinearLayout linearLayout22 = keyboardShortcutsLayout;
                    }
                    keyboardShortcutGroupsSize = keyboardShortcutGroupsSize4;
                    List<StringDrawableContainer> list = shortcutKeys;
                    shortcutKeyIconItemHeightWidth = shortcutKeyIconItemHeightWidth3;
                    i2 = i5;
                    int i6 = shortcutKeysSize;
                    categoryTitle = categoryTitle3;
                    itemsSize = itemsSize3;
                    keyboardShortcutGroupsSize2 = R.layout.keyboard_shortcuts_key_view;
                    shortcutContainer.addView(shortcutView);
                }
                j++;
                i4 = keyboardShortcutGroupsSize2;
                shortcutsKeyView2 = shortcutsKeyView;
                group2 = group;
                categoryTitle3 = categoryTitle;
                shortcutKeyIconItemHeightWidth3 = shortcutKeyIconItemHeightWidth;
                itemsSize3 = itemsSize;
                i5 = i2;
                keyboardShortcutGroupsSize4 = keyboardShortcutGroupsSize;
                LinearLayout linearLayout3 = keyboardShortcutsLayout;
                List<KeyboardShortcutGroup> list2 = keyboardShortcutGroups;
            }
            int keyboardShortcutGroupsSize5 = keyboardShortcutGroupsSize4;
            int keyboardShortcutGroupsSize6 = i4;
            TextView shortcutsKeyView3 = shortcutsKeyView2;
            int shortcutKeyIconItemHeightWidth4 = shortcutKeyIconItemHeightWidth3;
            KeyboardShortcutGroup keyboardShortcutGroup = group2;
            TextView textView = categoryTitle3;
            int i7 = itemsSize3;
            linearLayout = keyboardShortcutsLayout;
            linearLayout.addView(shortcutContainer);
            int i8 = i5;
            if (i8 < keyboardShortcutGroupsSize5 - 1) {
                z = false;
                linearLayout.addView(inflater.inflate(R.layout.keyboard_shortcuts_category_separator, linearLayout, false));
            } else {
                z = false;
            }
            i5 = i8 + 1;
            i4 = keyboardShortcutGroupsSize6;
            z2 = z;
            shortcutsKeyView2 = shortcutsKeyView3;
            shortcutKeyIconItemHeightWidth3 = shortcutKeyIconItemHeightWidth4;
            keyboardShortcutGroupsSize4 = keyboardShortcutGroupsSize5;
        }
        TextView textView2 = shortcutsKeyView2;
        int i9 = shortcutKeyIconItemHeightWidth3;
    }

    private List<StringDrawableContainer> getHumanReadableShortcutKeys(KeyboardShortcutInfo info) {
        String shortcutKeyString;
        List<StringDrawableContainer> shortcutKeys = getHumanReadableModifiers(info);
        if (shortcutKeys == null) {
            return null;
        }
        Drawable shortcutKeyDrawable = null;
        if (info.getBaseCharacter() > 0) {
            shortcutKeyString = String.valueOf(info.getBaseCharacter());
        } else if (this.mSpecialCharacterDrawables.get(info.getKeycode()) != null) {
            shortcutKeyDrawable = this.mSpecialCharacterDrawables.get(info.getKeycode());
            shortcutKeyString = this.mSpecialCharacterNames.get(info.getKeycode());
        } else if (this.mSpecialCharacterNames.get(info.getKeycode()) != null) {
            shortcutKeyString = this.mSpecialCharacterNames.get(info.getKeycode());
        } else if (info.getKeycode() == 0) {
            return shortcutKeys;
        } else {
            char displayLabel = this.mKeyCharacterMap.getDisplayLabel(info.getKeycode());
            if (displayLabel != 0) {
                shortcutKeyString = String.valueOf(displayLabel);
            } else {
                char displayLabel2 = this.mBackupKeyCharacterMap.getDisplayLabel(info.getKeycode());
                if (displayLabel2 == 0) {
                    return null;
                }
                shortcutKeyString = String.valueOf(displayLabel2);
            }
        }
        if (shortcutKeyString != null) {
            shortcutKeys.add(new StringDrawableContainer(shortcutKeyString, shortcutKeyDrawable));
        } else {
            Log.w(TAG, "Keyboard Shortcut does not have a text representation, skipping.");
        }
        return shortcutKeys;
    }

    private List<StringDrawableContainer> getHumanReadableModifiers(KeyboardShortcutInfo info) {
        List<StringDrawableContainer> shortcutKeys = new ArrayList<>();
        int modifiers = info.getModifiers();
        if (modifiers == 0) {
            return shortcutKeys;
        }
        for (int supportedModifier : this.mModifierList) {
            if ((modifiers & supportedModifier) != 0) {
                shortcutKeys.add(new StringDrawableContainer(this.mModifierNames.get(supportedModifier), this.mModifierDrawables.get(supportedModifier)));
                modifiers &= ~supportedModifier;
            }
        }
        if (modifiers != 0) {
            return null;
        }
        return shortcutKeys;
    }
}
