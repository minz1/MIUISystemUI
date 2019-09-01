package com.miui.internal.policy.impl.AwesomeLockScreenImp;

import miui.maml.data.ContentProviderBinder;
import miui.maml.data.VariableBinder;
import miui.maml.data.VariableBinderVisitor;

public class BlockedColumnsSetter extends VariableBinderVisitor {
    private String[] mColumns;
    private boolean mPrefix;
    private String mUri;

    public BlockedColumnsSetter(String uri, String... columns) {
        this(uri, false, columns);
    }

    public BlockedColumnsSetter(String uri, boolean uriPrefix, String... columns) {
        if (uri != null) {
            this.mPrefix = uriPrefix;
            this.mUri = uri;
            this.mColumns = columns;
            return;
        }
        throw new IllegalArgumentException("uri is null");
    }

    public void visit(VariableBinder vb) {
        if (vb instanceof ContentProviderBinder) {
            ContentProviderBinder cp = (ContentProviderBinder) vb;
            String uri = cp.getUriText();
            if (uri != null) {
                if ((this.mPrefix && uri.startsWith(this.mUri)) || uri.equals(this.mUri)) {
                    cp.setBlockedColumns(this.mColumns);
                }
            }
        }
    }
}
