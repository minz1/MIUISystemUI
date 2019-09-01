package com.android.systemui.miui.statusbar;

public class LocalScoreRule {
    public String desc;
    public int score;
    public String title;

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocalScoreRule scoreRule = (LocalScoreRule) o;
        if (this.score != scoreRule.score) {
            return false;
        }
        if (this.title == null ? scoreRule.title != null : !this.title.equals(scoreRule.title)) {
            return false;
        }
        if (this.desc != null) {
            z = this.desc.equals(scoreRule.desc);
        } else if (scoreRule.desc != null) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        int i = 0;
        int hashCode = 31 * (this.title != null ? this.title.hashCode() : 0);
        if (this.desc != null) {
            i = this.desc.hashCode();
        }
        return (31 * (hashCode + i)) + this.score;
    }

    public String toString() {
        return "LocalScoreRule{title='" + this.title + '\'' + ", desc='" + this.desc + '\'' + ", score=" + this.score + '}';
    }
}
