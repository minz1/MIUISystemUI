package com.android.keyguard.magazine.mode;

import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import org.json.JSONObject;

public class LockScreenMagazineWallpaperInfo {
    public String authority;
    public String btnText;
    public String content;
    public String cp;
    public String deeplinkUrl;
    public String entryTitle;
    public String ex;
    public boolean isTitleCustomized;
    public String key;
    public String landingPageUrl;
    public boolean like;
    public int linkType = 0;
    public String packageName;
    public int pos;
    public String provider;
    public String source;
    public boolean supportLike;
    public String tag;
    public String title;
    public String wallpaperUri;

    public void initExtra() {
        setLinkType();
        setEntryTitle();
        setTitleCustomized();
        setProvider();
        setSource();
    }

    public void setLinkType() {
        if (!TextUtils.isEmpty(this.ex)) {
            try {
                this.linkType = Integer.parseInt(new JSONObject(this.ex).getString("link_type"));
            } catch (Exception e) {
                this.linkType = 0;
            }
        }
    }

    public void setEntryTitle() {
        if (!TextUtils.isEmpty(this.ex)) {
            try {
                this.entryTitle = new JSONObject(this.ex).getString("lks_entry_text");
            } catch (Exception e) {
                this.entryTitle = null;
            }
        }
    }

    public void setTitleCustomized() {
        if (!TextUtils.isEmpty(this.ex)) {
            try {
                boolean z = true;
                if (new JSONObject(this.ex).getInt("title_customized") != 1) {
                    z = false;
                }
                this.isTitleCustomized = z;
            } catch (Exception e) {
                this.isTitleCustomized = false;
            }
        }
    }

    public void setProvider() {
        if (!TextUtils.isEmpty(this.ex)) {
            try {
                this.provider = new JSONObject(this.ex).getString("provider");
            } catch (Exception e) {
                this.provider = null;
            }
        }
    }

    public void setSource() {
        if (!TextUtils.isEmpty(this.ex)) {
            try {
                this.source = new JSONObject(this.ex).getString("source");
            } catch (Exception e) {
                this.source = null;
            }
        }
    }

    public String toString() {
        return "LockScreenMagazineWallpaperInfo [authority=" + this.authority + ", key=" + this.key + ", wallpaperUri=" + this.wallpaperUri + ", title=" + this.title + ", content=" + this.content + ", packageName=" + this.packageName + ", landingPageUrl=" + this.landingPageUrl + ", supportLike=" + this.supportLike + ", like=" + this.like + ", tag=" + this.tag + ", cp=" + this.cp + ", pos=" + this.pos + ", ex=" + this.ex + "]";
    }

    public boolean opendAd(Context context) {
        boolean opened = false;
        try {
            if (!TextUtils.isEmpty(this.deeplinkUrl)) {
                Intent intent = Intent.parseUri(this.deeplinkUrl, 0);
                if (!TextUtils.isEmpty(this.packageName)) {
                    intent.setPackage(this.packageName);
                }
                intent.putExtra("StartActivityWhenLocked", true);
                context.startActivityAsUser(intent, UserHandle.CURRENT);
                opened = true;
            }
        } catch (Exception e) {
            Log.e("wallpaperinfo", "deeplinkUrl not found : " + this.deeplinkUrl);
        }
        if (!opened) {
            try {
                if (!TextUtils.isEmpty(this.landingPageUrl)) {
                    Intent intent2 = Intent.parseUri(this.landingPageUrl, 0);
                    if (!TextUtils.isEmpty(this.packageName)) {
                        intent2.setPackage(this.packageName);
                    }
                    intent2.putExtra("StartActivityWhenLocked", true);
                    context.startActivityAsUser(intent2, UserHandle.CURRENT);
                    opened = true;
                }
            } catch (Exception e2) {
                Log.e("wallpaperinfo", "landingPageUrl not found : " + this.landingPageUrl);
            }
        }
        if (opened && !TextUtils.isEmpty(this.authority)) {
            IContentProvider provider2 = context.getContentResolver().acquireProvider(Uri.parse("content://" + this.authority));
            if (provider2 != null) {
                Log.d("wallpaperinfo", "tarck ad key=" + this.key + ";authority=" + this.authority);
                try {
                    JSONObject jo = new JSONObject();
                    jo.put("key", this.key);
                    jo.put("event", 2);
                    Bundle extras = new Bundle();
                    extras.putString("request_json", jo.toString());
                    provider2.call(context.getPackageName(), "recordEvent", null, extras);
                } catch (Exception e3) {
                    e3.printStackTrace();
                } catch (Throwable th) {
                    context.getContentResolver().releaseProvider(provider2);
                    throw th;
                }
                context.getContentResolver().releaseProvider(provider2);
            }
        }
        return opened;
    }
}
