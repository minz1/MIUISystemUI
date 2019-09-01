package com.android.systemui.statusbar.policy;

import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.Plugin;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.plugins.PluginManagerHelper;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.function.Consumer;
import com.android.systemui.util.function.Supplier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ExtensionControllerImpl implements ExtensionController {

    private class ExtensionBuilder<T> implements ExtensionController.ExtensionBuilder<T> {
        private ExtensionImpl<T> mExtension;

        private ExtensionBuilder() {
            this.mExtension = new ExtensionImpl<>();
        }

        public <P extends T> ExtensionController.ExtensionBuilder<T> withPlugin(Class<P> cls) {
            return withPlugin(cls, PluginManagerHelper.getAction(cls));
        }

        public <P extends T> ExtensionController.ExtensionBuilder<T> withPlugin(Class<P> cls, String action) {
            return withPlugin(cls, action, null);
        }

        public <P> ExtensionController.ExtensionBuilder<T> withPlugin(Class<P> cls, String action, ExtensionController.PluginConverter<T, P> converter) {
            if (!TextUtils.isEmpty(action)) {
                this.mExtension.addPlugin(action, cls, converter);
            }
            return this;
        }

        public ExtensionController.ExtensionBuilder<T> withDefault(Supplier<T> def) {
            this.mExtension.addDefault(def);
            return this;
        }

        public ExtensionController.ExtensionBuilder<T> withCallback(Consumer<T> callback) {
            this.mExtension.mCallbacks.add(callback);
            return this;
        }

        public ExtensionController.Extension build() {
            Collections.sort(this.mExtension.mProducers, new Comparator<Producer<T>>() {
                public int compare(Producer<T> o1, Producer<T> o2) {
                    if (o1 instanceof ExtensionImpl.PluginItem) {
                        return o2 instanceof ExtensionImpl.PluginItem ? 0 : -1;
                    }
                    if (!(o1 instanceof ExtensionImpl.TunerItem)) {
                        return 0;
                    }
                    if (o2 instanceof ExtensionImpl.PluginItem) {
                        return 1;
                    }
                    return o2 instanceof ExtensionImpl.TunerItem ? 0 : -1;
                }
            });
            this.mExtension.notifyChanged();
            return this.mExtension;
        }
    }

    private class ExtensionImpl<T> implements ExtensionController.Extension<T> {
        /* access modifiers changed from: private */
        public final ArrayList<Consumer<T>> mCallbacks;
        private T mItem;
        /* access modifiers changed from: private */
        public final ArrayList<Producer<T>> mProducers;

        private class Default<T> implements Producer<T> {
            private final Supplier<T> mSupplier;

            public Default(Supplier<T> supplier) {
                this.mSupplier = supplier;
            }

            public T get() {
                return this.mSupplier.get();
            }

            public void destroy() {
            }
        }

        private class PluginItem<P extends Plugin> implements PluginListener<P>, Producer<T> {
            private final ExtensionController.PluginConverter<T, P> mConverter;
            private T mItem;

            public PluginItem(String action, Class<P> cls, ExtensionController.PluginConverter<T, P> converter) {
                this.mConverter = converter;
                ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener(action, this, (Class<?>) cls);
            }

            /* JADX WARNING: type inference failed for: r2v0, types: [P, T, java.lang.Object] */
            /* JADX WARNING: Unknown variable types count: 1 */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onPluginConnected(P r2, android.content.Context r3) {
                /*
                    r1 = this;
                    com.android.systemui.statusbar.policy.ExtensionController$PluginConverter<T, P> r0 = r1.mConverter
                    if (r0 == 0) goto L_0x000d
                    com.android.systemui.statusbar.policy.ExtensionController$PluginConverter<T, P> r0 = r1.mConverter
                    java.lang.Object r0 = r0.getInterfaceFromPlugin(r2)
                    r1.mItem = r0
                    goto L_0x000f
                L_0x000d:
                    r1.mItem = r2
                L_0x000f:
                    com.android.systemui.statusbar.policy.ExtensionControllerImpl$ExtensionImpl r0 = com.android.systemui.statusbar.policy.ExtensionControllerImpl.ExtensionImpl.this
                    r0.notifyChanged()
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.ExtensionControllerImpl.ExtensionImpl.PluginItem.onPluginConnected(com.android.systemui.plugins.Plugin, android.content.Context):void");
            }

            public void onPluginDisconnected(P p) {
                this.mItem = null;
                ExtensionImpl.this.notifyChanged();
            }

            public T get() {
                return this.mItem;
            }

            public void destroy() {
                ((PluginManager) Dependency.get(PluginManager.class)).removePluginListener(this);
            }
        }

        private class TunerItem<T> implements Producer<T>, TunerService.Tunable {
            private final ExtensionController.TunerFactory<T> mFactory;
            private T mItem;
            private final ArrayMap<String, String> mSettings;
            final /* synthetic */ ExtensionImpl this$1;

            public T get() {
                return this.mItem;
            }

            public void destroy() {
                ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
            }

            public void onTuningChanged(String key, String newValue) {
                this.mSettings.put(key, newValue);
                this.mItem = this.mFactory.create(this.mSettings);
                this.this$1.notifyChanged();
            }
        }

        private ExtensionImpl() {
            this.mProducers = new ArrayList<>();
            this.mCallbacks = new ArrayList<>();
        }

        public T get() {
            return this.mItem;
        }

        public void destroy() {
            for (int i = 0; i < this.mProducers.size(); i++) {
                this.mProducers.get(i).destroy();
            }
        }

        /* access modifiers changed from: private */
        public void notifyChanged() {
            int i = 0;
            while (true) {
                if (i >= this.mProducers.size()) {
                    break;
                }
                T item = this.mProducers.get(i).get();
                if (item != null) {
                    this.mItem = item;
                    break;
                }
                i++;
            }
            for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
                this.mCallbacks.get(i2).accept(this.mItem);
            }
        }

        public void addDefault(Supplier<T> def) {
            this.mProducers.add(new Default(def));
        }

        public <P> void addPlugin(String action, Class<P> cls, ExtensionController.PluginConverter<T, P> converter) {
            this.mProducers.add(new PluginItem(action, cls, converter));
        }
    }

    private interface Producer<T> {
        void destroy();

        T get();
    }

    public <T> ExtensionBuilder<T> newExtension(Class<T> cls) {
        return new ExtensionBuilder<>();
    }
}
