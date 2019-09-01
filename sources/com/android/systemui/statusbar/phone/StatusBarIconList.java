package com.android.systemui.statusbar.phone;

import com.android.internal.statusbar.StatusBarIcon;
import java.io.PrintWriter;
import java.util.ArrayList;

public class StatusBarIconList {
    protected ArrayList<StatusBarIcon> mIcons = new ArrayList<>();
    protected ArrayList<String> mSlots = new ArrayList<>();

    public StatusBarIconList(String[] slots) {
        for (String add : slots) {
            this.mSlots.add(add);
            this.mIcons.add(null);
        }
    }

    public int getSlotIndex(String slot) {
        int N = this.mSlots.size();
        for (int i = 0; i < N; i++) {
            if (slot.equals(this.mSlots.get(i))) {
                return i;
            }
        }
        this.mSlots.add(0, slot);
        this.mIcons.add(0, null);
        return 0;
    }

    public void setIcon(int index, StatusBarIcon icon) {
        this.mIcons.set(index, icon);
    }

    public void removeIcon(int index) {
        this.mIcons.set(index, null);
    }

    public String getSlot(int index) {
        return this.mSlots.get(index);
    }

    public StatusBarIcon getIcon(int index) {
        return this.mIcons.get(index);
    }

    public int getViewIndex(int index) {
        int count = 0;
        for (int i = 0; i < index; i++) {
            if (this.mIcons.get(i) != null) {
                count++;
            }
        }
        return count;
    }

    public void dump(PrintWriter pw) {
        int N = this.mSlots.size();
        pw.println("  icon slots: " + N);
        for (int i = 0; i < N; i++) {
            pw.printf("    %2d: (%s) %s\n", new Object[]{Integer.valueOf(i), this.mSlots.get(i), this.mIcons.get(i)});
        }
    }
}
