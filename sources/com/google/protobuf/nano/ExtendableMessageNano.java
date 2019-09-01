package com.google.protobuf.nano;

import com.google.protobuf.nano.ExtendableMessageNano;

public abstract class ExtendableMessageNano<M extends ExtendableMessageNano<M>> extends MessageNano {
    protected FieldArray unknownFieldData;

    public M clone() throws CloneNotSupportedException {
        M cloned = (ExtendableMessageNano) super.clone();
        InternalNano.cloneUnknownFieldData(this, cloned);
        return cloned;
    }
}
