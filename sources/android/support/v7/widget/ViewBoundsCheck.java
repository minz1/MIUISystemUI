package android.support.v7.widget;

import android.view.View;

class ViewBoundsCheck {
    BoundFlags mBoundFlags = new BoundFlags();
    final Callback mCallback;

    static class BoundFlags {
        int mBoundFlags = 0;
        int mChildEnd;
        int mChildStart;
        int mRvEnd;
        int mRvStart;

        BoundFlags() {
        }

        /* access modifiers changed from: package-private */
        public void setBounds(int rvStart, int rvEnd, int childStart, int childEnd) {
            this.mRvStart = rvStart;
            this.mRvEnd = rvEnd;
            this.mChildStart = childStart;
            this.mChildEnd = childEnd;
        }

        /* access modifiers changed from: package-private */
        public void addFlags(int flags) {
            this.mBoundFlags |= flags;
        }

        /* access modifiers changed from: package-private */
        public void resetFlags() {
            this.mBoundFlags = 0;
        }

        /* access modifiers changed from: package-private */
        public int compare(int x, int y) {
            if (x > y) {
                return 1;
            }
            if (x == y) {
                return 2;
            }
            return 4;
        }

        /* access modifiers changed from: package-private */
        public boolean boundsMatch() {
            if ((this.mBoundFlags & 7) != 0 && (this.mBoundFlags & (compare(this.mChildStart, this.mRvStart) << 0)) == 0) {
                return false;
            }
            if ((this.mBoundFlags & 112) != 0 && (this.mBoundFlags & (compare(this.mChildStart, this.mRvEnd) << 4)) == 0) {
                return false;
            }
            if ((this.mBoundFlags & 1792) != 0 && (this.mBoundFlags & (compare(this.mChildEnd, this.mRvStart) << 8)) == 0) {
                return false;
            }
            if ((this.mBoundFlags & 28672) == 0 || (this.mBoundFlags & (compare(this.mChildEnd, this.mRvEnd) << 12)) != 0) {
                return true;
            }
            return false;
        }
    }

    interface Callback {
        View getChildAt(int i);

        int getChildEnd(View view);

        int getChildStart(View view);

        int getParentEnd();

        int getParentStart();
    }

    ViewBoundsCheck(Callback callback) {
        this.mCallback = callback;
    }

    /* access modifiers changed from: package-private */
    public View findOneViewWithinBoundFlags(int fromIndex, int toIndex, int preferredBoundFlags, int acceptableBoundFlags) {
        int start = this.mCallback.getParentStart();
        int end = this.mCallback.getParentEnd();
        int next = toIndex > fromIndex ? 1 : -1;
        View acceptableMatch = null;
        for (int i = fromIndex; i != toIndex; i += next) {
            View child = this.mCallback.getChildAt(i);
            this.mBoundFlags.setBounds(start, end, this.mCallback.getChildStart(child), this.mCallback.getChildEnd(child));
            if (preferredBoundFlags != 0) {
                this.mBoundFlags.resetFlags();
                this.mBoundFlags.addFlags(preferredBoundFlags);
                if (this.mBoundFlags.boundsMatch()) {
                    return child;
                }
            }
            if (acceptableBoundFlags != 0) {
                this.mBoundFlags.resetFlags();
                this.mBoundFlags.addFlags(acceptableBoundFlags);
                if (this.mBoundFlags.boundsMatch()) {
                    acceptableMatch = child;
                }
            }
        }
        return acceptableMatch;
    }

    /* access modifiers changed from: package-private */
    public boolean isViewWithinBoundFlags(View child, int boundsFlags) {
        this.mBoundFlags.setBounds(this.mCallback.getParentStart(), this.mCallback.getParentEnd(), this.mCallback.getChildStart(child), this.mCallback.getChildEnd(child));
        if (boundsFlags == 0) {
            return false;
        }
        this.mBoundFlags.resetFlags();
        this.mBoundFlags.addFlags(boundsFlags);
        return this.mBoundFlags.boundsMatch();
    }
}
