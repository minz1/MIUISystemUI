package com.miui.systemui.support.v4.app;

import android.graphics.Rect;
import android.os.Build;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import com.miui.systemui.support.v4.util.ArrayMap;
import java.util.ArrayList;
import java.util.Collection;

class FragmentTransition {
    private static final int[] INVERSE_OPS = {0, 3, 0, 1, 5, 4, 7, 6, 9, 8};

    static class FragmentContainerTransition {
        public Fragment firstOut;
        public boolean firstOutIsPop;
        public BackStackRecord firstOutTransaction;
        public Fragment lastIn;
        public boolean lastInIsPop;
        public BackStackRecord lastInTransaction;

        FragmentContainerTransition() {
        }
    }

    static void startTransitions(FragmentManagerImpl fragmentManager, ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, int startIndex, int endIndex, boolean isReordered) {
        if (fragmentManager.mCurState >= 1 && Build.VERSION.SDK_INT >= 21) {
            SparseArray<FragmentContainerTransition> transitioningFragments = new SparseArray<>();
            for (int i = startIndex; i < endIndex; i++) {
                BackStackRecord record = records.get(i);
                if (isRecordPop.get(i).booleanValue()) {
                    calculatePopFragments(record, transitioningFragments, isReordered);
                } else {
                    calculateFragments(record, transitioningFragments, isReordered);
                }
            }
            if (transitioningFragments.size() != 0) {
                View nonExistentView = new View(fragmentManager.mHost.getContext());
                int numContainers = transitioningFragments.size();
                for (int i2 = 0; i2 < numContainers; i2++) {
                    int containerId = transitioningFragments.keyAt(i2);
                    ArrayMap<String, String> nameOverrides = calculateNameOverrides(containerId, records, isRecordPop, startIndex, endIndex);
                    FragmentContainerTransition containerTransition = transitioningFragments.valueAt(i2);
                    if (isReordered) {
                        configureTransitionsReordered(fragmentManager, containerId, containerTransition, nonExistentView, nameOverrides);
                    } else {
                        configureTransitionsOrdered(fragmentManager, containerId, containerTransition, nonExistentView, nameOverrides);
                    }
                }
            }
        }
    }

    private static ArrayMap<String, String> calculateNameOverrides(int containerId, ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        ArrayList<String> sources;
        ArrayList<String> targets;
        ArrayMap<String, String> nameOverrides = new ArrayMap<>();
        for (int recordNum = endIndex - 1; recordNum >= startIndex; recordNum--) {
            BackStackRecord record = records.get(recordNum);
            if (record.interactsWith(containerId)) {
                boolean isPop = isRecordPop.get(recordNum).booleanValue();
                if (record.mSharedElementSourceNames != null) {
                    int numSharedElements = record.mSharedElementSourceNames.size();
                    if (isPop) {
                        targets = record.mSharedElementSourceNames;
                        sources = record.mSharedElementTargetNames;
                    } else {
                        sources = record.mSharedElementSourceNames;
                        targets = record.mSharedElementTargetNames;
                    }
                    for (int i = 0; i < numSharedElements; i++) {
                        String sourceName = sources.get(i);
                        String targetName = targets.get(i);
                        String previousTarget = nameOverrides.remove(targetName);
                        if (previousTarget != null) {
                            nameOverrides.put(sourceName, previousTarget);
                        } else {
                            nameOverrides.put(sourceName, targetName);
                        }
                    }
                }
            }
        }
        return nameOverrides;
    }

    /* JADX WARNING: type inference failed for: r2v6, types: [android.view.View] */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void configureTransitionsReordered(com.miui.systemui.support.v4.app.FragmentManagerImpl r27, int r28, com.miui.systemui.support.v4.app.FragmentTransition.FragmentContainerTransition r29, android.view.View r30, com.miui.systemui.support.v4.util.ArrayMap<java.lang.String, java.lang.String> r31) {
        /*
            r0 = r27
            r9 = r29
            r10 = r30
            r1 = 0
            com.miui.systemui.support.v4.app.FragmentContainer r2 = r0.mContainer
            boolean r2 = r2.onHasView()
            if (r2 == 0) goto L_0x001b
            com.miui.systemui.support.v4.app.FragmentContainer r2 = r0.mContainer
            r11 = r28
            android.view.View r2 = r2.onFindViewById(r11)
            r1 = r2
            android.view.ViewGroup r1 = (android.view.ViewGroup) r1
            goto L_0x001d
        L_0x001b:
            r11 = r28
        L_0x001d:
            r12 = r1
            if (r12 != 0) goto L_0x0021
            return
        L_0x0021:
            com.miui.systemui.support.v4.app.Fragment r13 = r9.lastIn
            com.miui.systemui.support.v4.app.Fragment r14 = r9.firstOut
            boolean r15 = r9.lastInIsPop
            boolean r8 = r9.firstOutIsPop
            java.util.ArrayList r1 = new java.util.ArrayList
            r1.<init>()
            r7 = r1
            java.util.ArrayList r1 = new java.util.ArrayList
            r1.<init>()
            r6 = r1
            java.lang.Object r5 = getEnterTransition(r13, r15)
            java.lang.Object r4 = getExitTransition(r14, r8)
            r1 = r12
            r2 = r10
            r3 = r31
            r23 = r4
            r4 = r9
            r24 = r5
            r5 = r6
            r0 = r6
            r6 = r7
            r9 = r7
            r7 = r24
            r25 = r8
            r8 = r23
            java.lang.Object r1 = configureSharedElementsReordered(r1, r2, r3, r4, r5, r6, r7, r8)
            r2 = r24
            if (r2 != 0) goto L_0x005f
            if (r1 != 0) goto L_0x005f
            r3 = r23
            if (r3 != 0) goto L_0x0061
            return
        L_0x005f:
            r3 = r23
        L_0x0061:
            java.util.ArrayList r4 = configureEnteringExitingViews(r3, r14, r0, r10)
            java.util.ArrayList r5 = configureEnteringExitingViews(r2, r13, r9, r10)
            r6 = 4
            setViewVisibility(r5, r6)
            java.lang.Object r6 = mergeTransitions(r2, r3, r1, r13, r15)
            if (r6 == 0) goto L_0x009e
            replaceHide(r3, r14, r4)
            java.util.ArrayList r7 = com.miui.systemui.support.v4.app.FragmentTransitionCompat21.prepareSetNameOverridesReordered(r9)
            r16 = r6
            r17 = r2
            r18 = r5
            r19 = r3
            r20 = r4
            r21 = r1
            r22 = r9
            com.miui.systemui.support.v4.app.FragmentTransitionCompat21.scheduleRemoveTargets(r16, r17, r18, r19, r20, r21, r22)
            com.miui.systemui.support.v4.app.FragmentTransitionCompat21.beginDelayedTransition(r12, r6)
            r8 = r31
            com.miui.systemui.support.v4.app.FragmentTransitionCompat21.setNameOverridesReordered(r12, r0, r9, r7, r8)
            r26 = r2
            r2 = 0
            setViewVisibility(r5, r2)
            com.miui.systemui.support.v4.app.FragmentTransitionCompat21.swapSharedElementTargets(r1, r0, r9)
            goto L_0x00a2
        L_0x009e:
            r8 = r31
            r26 = r2
        L_0x00a2:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.systemui.support.v4.app.FragmentTransition.configureTransitionsReordered(com.miui.systemui.support.v4.app.FragmentManagerImpl, int, com.miui.systemui.support.v4.app.FragmentTransition$FragmentContainerTransition, android.view.View, com.miui.systemui.support.v4.util.ArrayMap):void");
    }

    private static void replaceHide(Object exitTransition, Fragment exitingFragment, final ArrayList<View> exitingViews) {
        if (exitingFragment != null && exitTransition != null && exitingFragment.mAdded && exitingFragment.mHidden && exitingFragment.mHiddenChanged) {
            exitingFragment.setHideReplaced(true);
            FragmentTransitionCompat21.scheduleHideFragmentView(exitTransition, exitingFragment.getView(), exitingViews);
            OneShotPreDrawListener.add(exitingFragment.mContainer, new Runnable() {
                public void run() {
                    FragmentTransition.setViewVisibility(exitingViews, 4);
                }
            });
        }
    }

    /* JADX WARNING: type inference failed for: r2v6, types: [android.view.View] */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void configureTransitionsOrdered(com.miui.systemui.support.v4.app.FragmentManagerImpl r30, int r31, com.miui.systemui.support.v4.app.FragmentTransition.FragmentContainerTransition r32, android.view.View r33, com.miui.systemui.support.v4.util.ArrayMap<java.lang.String, java.lang.String> r34) {
        /*
            r0 = r30
            r9 = r32
            r10 = r33
            r11 = r34
            r1 = 0
            com.miui.systemui.support.v4.app.FragmentContainer r2 = r0.mContainer
            boolean r2 = r2.onHasView()
            if (r2 == 0) goto L_0x001d
            com.miui.systemui.support.v4.app.FragmentContainer r2 = r0.mContainer
            r12 = r31
            android.view.View r2 = r2.onFindViewById(r12)
            r1 = r2
            android.view.ViewGroup r1 = (android.view.ViewGroup) r1
            goto L_0x001f
        L_0x001d:
            r12 = r31
        L_0x001f:
            r13 = r1
            if (r13 != 0) goto L_0x0023
            return
        L_0x0023:
            com.miui.systemui.support.v4.app.Fragment r14 = r9.lastIn
            com.miui.systemui.support.v4.app.Fragment r15 = r9.firstOut
            boolean r8 = r9.lastInIsPop
            boolean r7 = r9.firstOutIsPop
            java.lang.Object r6 = getEnterTransition(r14, r8)
            java.lang.Object r5 = getExitTransition(r15, r7)
            java.util.ArrayList r1 = new java.util.ArrayList
            r1.<init>()
            r4 = r1
            java.util.ArrayList r1 = new java.util.ArrayList
            r1.<init>()
            r3 = r1
            r1 = r13
            r2 = r10
            r23 = r3
            r3 = r11
            r24 = r4
            r4 = r9
            r25 = r5
            r5 = r24
            r26 = r6
            r6 = r23
            r27 = r7
            r7 = r26
            r28 = r8
            r8 = r25
            java.lang.Object r8 = configureSharedElementsOrdered(r1, r2, r3, r4, r5, r6, r7, r8)
            if (r7 != 0) goto L_0x0064
            if (r8 != 0) goto L_0x0064
            r1 = r25
            if (r1 != 0) goto L_0x0066
            return
        L_0x0064:
            r1 = r25
        L_0x0066:
            r6 = r24
            java.util.ArrayList r5 = configureEnteringExitingViews(r1, r15, r6, r10)
            if (r5 == 0) goto L_0x0077
            boolean r2 = r5.isEmpty()
            if (r2 == 0) goto L_0x0075
            goto L_0x0077
        L_0x0075:
            r4 = r1
            goto L_0x0079
        L_0x0077:
            r1 = 0
            goto L_0x0075
        L_0x0079:
            com.miui.systemui.support.v4.app.FragmentTransitionCompat21.addTarget(r7, r10)
            boolean r1 = r9.lastInIsPop
            java.lang.Object r3 = mergeTransitions(r7, r4, r8, r14, r1)
            if (r3 == 0) goto L_0x00be
            java.util.ArrayList r1 = new java.util.ArrayList
            r1.<init>()
            r18 = r1
            r16 = r3
            r17 = r7
            r19 = r4
            r20 = r5
            r21 = r8
            r22 = r23
            com.miui.systemui.support.v4.app.FragmentTransitionCompat21.scheduleRemoveTargets(r16, r17, r18, r19, r20, r21, r22)
            r1 = r13
            r2 = r14
            r0 = r3
            r3 = r10
            r16 = r4
            r4 = r23
            r17 = r5
            r5 = r7
            r19 = r6
            r6 = r18
            r20 = r7
            r7 = r16
            r8 = r17
            scheduleTargetChange(r1, r2, r3, r4, r5, r6, r7, r8)
            r1 = r23
            com.miui.systemui.support.v4.app.FragmentTransitionCompat21.setNameOverridesOrdered(r13, r1, r11)
            com.miui.systemui.support.v4.app.FragmentTransitionCompat21.beginDelayedTransition(r13, r0)
            com.miui.systemui.support.v4.app.FragmentTransitionCompat21.scheduleNameReset(r13, r1, r11)
            goto L_0x00cb
        L_0x00be:
            r0 = r3
            r16 = r4
            r17 = r5
            r19 = r6
            r20 = r7
            r21 = r8
            r1 = r23
        L_0x00cb:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.systemui.support.v4.app.FragmentTransition.configureTransitionsOrdered(com.miui.systemui.support.v4.app.FragmentManagerImpl, int, com.miui.systemui.support.v4.app.FragmentTransition$FragmentContainerTransition, android.view.View, com.miui.systemui.support.v4.util.ArrayMap):void");
    }

    private static void scheduleTargetChange(ViewGroup sceneRoot, Fragment inFragment, View nonExistentView, ArrayList<View> sharedElementsIn, Object enterTransition, ArrayList<View> enteringViews, Object exitTransition, ArrayList<View> exitingViews) {
        final Object obj = enterTransition;
        final View view = nonExistentView;
        final Fragment fragment = inFragment;
        final ArrayList<View> arrayList = sharedElementsIn;
        final ArrayList<View> arrayList2 = enteringViews;
        final ArrayList<View> arrayList3 = exitingViews;
        final Object obj2 = exitTransition;
        AnonymousClass2 r0 = new Runnable() {
            public void run() {
                if (obj != null) {
                    FragmentTransitionCompat21.removeTarget(obj, view);
                    arrayList2.addAll(FragmentTransition.configureEnteringExitingViews(obj, fragment, arrayList, view));
                }
                if (arrayList3 != null) {
                    if (obj2 != null) {
                        ArrayList<View> tempExiting = new ArrayList<>();
                        tempExiting.add(view);
                        FragmentTransitionCompat21.replaceTargets(obj2, arrayList3, tempExiting);
                    }
                    arrayList3.clear();
                    arrayList3.add(view);
                }
            }
        };
        OneShotPreDrawListener.add(sceneRoot, r0);
    }

    private static Object getSharedElementTransition(Fragment inFragment, Fragment outFragment, boolean isPop) {
        Object obj;
        if (inFragment == null || outFragment == null) {
            return null;
        }
        if (isPop) {
            obj = outFragment.getSharedElementReturnTransition();
        } else {
            obj = inFragment.getSharedElementEnterTransition();
        }
        return FragmentTransitionCompat21.wrapTransitionInSet(FragmentTransitionCompat21.cloneTransition(obj));
    }

    private static Object getEnterTransition(Fragment inFragment, boolean isPop) {
        Object obj;
        if (inFragment == null) {
            return null;
        }
        if (isPop) {
            obj = inFragment.getReenterTransition();
        } else {
            obj = inFragment.getEnterTransition();
        }
        return FragmentTransitionCompat21.cloneTransition(obj);
    }

    private static Object getExitTransition(Fragment outFragment, boolean isPop) {
        Object obj;
        if (outFragment == null) {
            return null;
        }
        if (isPop) {
            obj = outFragment.getReturnTransition();
        } else {
            obj = outFragment.getExitTransition();
        }
        return FragmentTransitionCompat21.cloneTransition(obj);
    }

    private static Object configureSharedElementsReordered(ViewGroup sceneRoot, View nonExistentView, ArrayMap<String, String> nameOverrides, FragmentContainerTransition fragments, ArrayList<View> sharedElementsOut, ArrayList<View> sharedElementsIn, Object enterTransition, Object exitTransition) {
        Rect epicenter;
        final View epicenterView;
        View view = nonExistentView;
        ArrayMap<String, String> arrayMap = nameOverrides;
        FragmentContainerTransition fragmentContainerTransition = fragments;
        ArrayList<View> arrayList = sharedElementsOut;
        ArrayList<View> arrayList2 = sharedElementsIn;
        Object obj = enterTransition;
        Object obj2 = exitTransition;
        Fragment inFragment = fragmentContainerTransition.lastIn;
        Fragment outFragment = fragmentContainerTransition.firstOut;
        if (inFragment != null) {
            inFragment.getView().setVisibility(0);
        }
        if (inFragment == null || outFragment == null) {
            ViewGroup viewGroup = sceneRoot;
            return null;
        }
        boolean inIsPop = fragmentContainerTransition.lastInIsPop;
        Object sharedElementTransition = nameOverrides.isEmpty() ? null : getSharedElementTransition(inFragment, outFragment, inIsPop);
        ArrayMap<String, View> outSharedElements = captureOutSharedElements(arrayMap, sharedElementTransition, fragmentContainerTransition);
        ArrayMap<String, View> inSharedElements = captureInSharedElements(arrayMap, sharedElementTransition, fragmentContainerTransition);
        if (nameOverrides.isEmpty()) {
            sharedElementTransition = null;
            if (outSharedElements != null) {
                outSharedElements.clear();
            }
            if (inSharedElements != null) {
                inSharedElements.clear();
            }
        } else {
            addSharedElementsWithMatchingNames(arrayList, outSharedElements, nameOverrides.keySet());
            addSharedElementsWithMatchingNames(arrayList2, inSharedElements, nameOverrides.values());
        }
        Object sharedElementTransition2 = sharedElementTransition;
        if (obj == null && obj2 == null && sharedElementTransition2 == null) {
            return null;
        }
        callSharedElementStartEnd(inFragment, outFragment, inIsPop, outSharedElements, true);
        if (sharedElementTransition2 != null) {
            arrayList2.add(view);
            FragmentTransitionCompat21.setSharedElementTargets(sharedElementTransition2, view, arrayList);
            setOutEpicenter(sharedElementTransition2, obj2, outSharedElements, fragmentContainerTransition.firstOutIsPop, fragmentContainerTransition.firstOutTransaction);
            Rect epicenter2 = new Rect();
            View epicenterView2 = getInEpicenterView(inSharedElements, fragmentContainerTransition, obj, inIsPop);
            if (epicenterView2 != null) {
                FragmentTransitionCompat21.setEpicenter(obj, epicenter2);
            }
            View view2 = epicenterView2;
            epicenter = epicenter2;
            epicenterView = view2;
        } else {
            epicenterView = null;
            epicenter = null;
        }
        AnonymousClass3 r0 = r7;
        final Fragment fragment = inFragment;
        Object sharedElementTransition3 = sharedElementTransition2;
        final Fragment fragment2 = outFragment;
        ArrayMap<String, View> inSharedElements2 = inSharedElements;
        final boolean z = inIsPop;
        ArrayMap<String, View> arrayMap2 = outSharedElements;
        final ArrayMap<String, View> outSharedElements2 = inSharedElements2;
        boolean z2 = inIsPop;
        final Rect rect = epicenter;
        AnonymousClass3 r7 = new Runnable() {
            public void run() {
                FragmentTransition.callSharedElementStartEnd(Fragment.this, fragment2, z, outSharedElements2, false);
                if (epicenterView != null) {
                    FragmentTransitionCompat21.getBoundsOnScreen(epicenterView, rect);
                }
            }
        };
        OneShotPreDrawListener.add(sceneRoot, r0);
        return sharedElementTransition3;
    }

    private static void addSharedElementsWithMatchingNames(ArrayList<View> views, ArrayMap<String, View> sharedElements, Collection<String> nameOverridesSet) {
        for (int i = sharedElements.size() - 1; i >= 0; i--) {
            View view = sharedElements.valueAt(i);
            if (nameOverridesSet.contains(view.getTransitionName())) {
                views.add(view);
            }
        }
    }

    private static Object configureSharedElementsOrdered(ViewGroup sceneRoot, View nonExistentView, ArrayMap<String, String> nameOverrides, FragmentContainerTransition fragments, ArrayList<View> sharedElementsOut, ArrayList<View> sharedElementsIn, Object enterTransition, Object exitTransition) {
        FragmentContainerTransition fragmentContainerTransition = fragments;
        ArrayList<View> arrayList = sharedElementsOut;
        Object obj = enterTransition;
        Object obj2 = exitTransition;
        Fragment inFragment = fragmentContainerTransition.lastIn;
        Fragment outFragment = fragmentContainerTransition.firstOut;
        Rect inEpicenter = null;
        if (inFragment == null) {
            ViewGroup viewGroup = sceneRoot;
            Fragment fragment = outFragment;
            Fragment fragment2 = inFragment;
        } else if (outFragment == null) {
            ViewGroup viewGroup2 = sceneRoot;
            Fragment fragment3 = outFragment;
            Fragment fragment4 = inFragment;
        } else {
            final boolean inIsPop = fragmentContainerTransition.lastInIsPop;
            Object sharedElementTransition = nameOverrides.isEmpty() ? null : getSharedElementTransition(inFragment, outFragment, inIsPop);
            ArrayMap<String, String> arrayMap = nameOverrides;
            ArrayMap<String, View> outSharedElements = captureOutSharedElements(arrayMap, sharedElementTransition, fragmentContainerTransition);
            if (nameOverrides.isEmpty()) {
                sharedElementTransition = null;
            } else {
                arrayList.addAll(outSharedElements.values());
            }
            Object sharedElementTransition2 = sharedElementTransition;
            if (obj == null && obj2 == null && sharedElementTransition2 == null) {
                return null;
            }
            callSharedElementStartEnd(inFragment, outFragment, inIsPop, outSharedElements, true);
            if (sharedElementTransition2 != null) {
                inEpicenter = new Rect();
                FragmentTransitionCompat21.setSharedElementTargets(sharedElementTransition2, nonExistentView, arrayList);
                setOutEpicenter(sharedElementTransition2, obj2, outSharedElements, fragmentContainerTransition.firstOutIsPop, fragmentContainerTransition.firstOutTransaction);
                if (obj != null) {
                    FragmentTransitionCompat21.setEpicenter(obj, inEpicenter);
                }
            } else {
                View view = nonExistentView;
            }
            final Rect inEpicenter2 = inEpicenter;
            final Object finalSharedElementTransition = sharedElementTransition2;
            final ArrayMap<String, String> arrayMap2 = arrayMap;
            AnonymousClass4 r15 = r0;
            final FragmentContainerTransition fragmentContainerTransition2 = fragmentContainerTransition;
            final ArrayList<View> arrayList2 = sharedElementsIn;
            Object sharedElementTransition3 = sharedElementTransition2;
            final View view2 = nonExistentView;
            ArrayMap<String, View> arrayMap3 = outSharedElements;
            final Fragment fragment5 = inFragment;
            final Fragment fragment6 = outFragment;
            boolean z = inIsPop;
            Fragment fragment7 = outFragment;
            final ArrayList<View> arrayList3 = arrayList;
            Fragment fragment8 = inFragment;
            final Object obj3 = obj;
            AnonymousClass4 r0 = new Runnable() {
                public void run() {
                    ArrayMap<String, View> inSharedElements = FragmentTransition.captureInSharedElements(ArrayMap.this, finalSharedElementTransition, fragmentContainerTransition2);
                    if (inSharedElements != null) {
                        arrayList2.addAll(inSharedElements.values());
                        arrayList2.add(view2);
                    }
                    FragmentTransition.callSharedElementStartEnd(fragment5, fragment6, inIsPop, inSharedElements, false);
                    if (finalSharedElementTransition != null) {
                        FragmentTransitionCompat21.swapSharedElementTargets(finalSharedElementTransition, arrayList3, arrayList2);
                        View inEpicenterView = FragmentTransition.getInEpicenterView(inSharedElements, fragmentContainerTransition2, obj3, inIsPop);
                        if (inEpicenterView != null) {
                            FragmentTransitionCompat21.getBoundsOnScreen(inEpicenterView, inEpicenter2);
                        }
                    }
                }
            };
            OneShotPreDrawListener.add(sceneRoot, r15);
            return sharedElementTransition3;
        }
        return null;
    }

    private static ArrayMap<String, View> captureOutSharedElements(ArrayMap<String, String> nameOverrides, Object sharedElementTransition, FragmentContainerTransition fragments) {
        ArrayList<String> names;
        SharedElementCallback sharedElementCallback;
        if (nameOverrides.isEmpty() || sharedElementTransition == null) {
            nameOverrides.clear();
            return null;
        }
        Fragment outFragment = fragments.firstOut;
        ArrayMap<String, View> outSharedElements = new ArrayMap<>();
        FragmentTransitionCompat21.findNamedViews(outSharedElements, outFragment.getView());
        BackStackRecord outTransaction = fragments.firstOutTransaction;
        if (fragments.firstOutIsPop) {
            sharedElementCallback = outFragment.getEnterTransitionCallback();
            names = outTransaction.mSharedElementTargetNames;
        } else {
            sharedElementCallback = outFragment.getExitTransitionCallback();
            names = outTransaction.mSharedElementSourceNames;
        }
        outSharedElements.retainAll(names);
        if (sharedElementCallback != null) {
            sharedElementCallback.onMapSharedElements(names, outSharedElements);
            for (int i = names.size() - 1; i >= 0; i--) {
                String name = names.get(i);
                View view = outSharedElements.get(name);
                if (view == null) {
                    nameOverrides.remove(name);
                } else if (!name.equals(view.getTransitionName())) {
                    nameOverrides.put(view.getTransitionName(), nameOverrides.remove(name));
                }
            }
        } else {
            nameOverrides.retainAll(outSharedElements.keySet());
        }
        return outSharedElements;
    }

    /* access modifiers changed from: private */
    public static ArrayMap<String, View> captureInSharedElements(ArrayMap<String, String> nameOverrides, Object sharedElementTransition, FragmentContainerTransition fragments) {
        ArrayList<String> names;
        SharedElementCallback sharedElementCallback;
        Fragment inFragment = fragments.lastIn;
        View fragmentView = inFragment.getView();
        if (nameOverrides.isEmpty() || sharedElementTransition == null || fragmentView == null) {
            nameOverrides.clear();
            return null;
        }
        ArrayMap<String, View> inSharedElements = new ArrayMap<>();
        FragmentTransitionCompat21.findNamedViews(inSharedElements, fragmentView);
        BackStackRecord inTransaction = fragments.lastInTransaction;
        if (fragments.lastInIsPop) {
            sharedElementCallback = inFragment.getExitTransitionCallback();
            names = inTransaction.mSharedElementSourceNames;
        } else {
            sharedElementCallback = inFragment.getEnterTransitionCallback();
            names = inTransaction.mSharedElementTargetNames;
        }
        if (names != null) {
            inSharedElements.retainAll(names);
        }
        if (sharedElementCallback != null) {
            sharedElementCallback.onMapSharedElements(names, inSharedElements);
            for (int i = names.size() - 1; i >= 0; i--) {
                String name = names.get(i);
                View view = inSharedElements.get(name);
                if (view == null) {
                    String key = findKeyForValue(nameOverrides, name);
                    if (key != null) {
                        nameOverrides.remove(key);
                    }
                } else if (!name.equals(view.getTransitionName())) {
                    String key2 = findKeyForValue(nameOverrides, name);
                    if (key2 != null) {
                        nameOverrides.put(key2, view.getTransitionName());
                    }
                }
            }
        } else {
            retainValues(nameOverrides, inSharedElements);
        }
        return inSharedElements;
    }

    private static String findKeyForValue(ArrayMap<String, String> map, String value) {
        int numElements = map.size();
        for (int i = 0; i < numElements; i++) {
            if (value.equals(map.valueAt(i))) {
                return map.keyAt(i);
            }
        }
        return null;
    }

    /* access modifiers changed from: private */
    public static View getInEpicenterView(ArrayMap<String, View> inSharedElements, FragmentContainerTransition fragments, Object enterTransition, boolean inIsPop) {
        String targetName;
        BackStackRecord inTransaction = fragments.lastInTransaction;
        if (enterTransition == null || inSharedElements == null || inTransaction.mSharedElementSourceNames == null || inTransaction.mSharedElementSourceNames.isEmpty()) {
            return null;
        }
        if (inIsPop) {
            targetName = inTransaction.mSharedElementSourceNames.get(0);
        } else {
            targetName = inTransaction.mSharedElementTargetNames.get(0);
        }
        return inSharedElements.get(targetName);
    }

    private static void setOutEpicenter(Object sharedElementTransition, Object exitTransition, ArrayMap<String, View> outSharedElements, boolean outIsPop, BackStackRecord outTransaction) {
        String sourceName;
        if (outTransaction.mSharedElementSourceNames != null && !outTransaction.mSharedElementSourceNames.isEmpty()) {
            if (outIsPop) {
                sourceName = outTransaction.mSharedElementTargetNames.get(0);
            } else {
                sourceName = outTransaction.mSharedElementSourceNames.get(0);
            }
            View outEpicenterView = outSharedElements.get(sourceName);
            FragmentTransitionCompat21.setEpicenter(sharedElementTransition, outEpicenterView);
            if (exitTransition != null) {
                FragmentTransitionCompat21.setEpicenter(exitTransition, outEpicenterView);
            }
        }
    }

    private static void retainValues(ArrayMap<String, String> nameOverrides, ArrayMap<String, View> namedViews) {
        for (int i = nameOverrides.size() - 1; i >= 0; i--) {
            if (!namedViews.containsKey(nameOverrides.valueAt(i))) {
                nameOverrides.removeAt(i);
            }
        }
    }

    /* access modifiers changed from: private */
    public static void callSharedElementStartEnd(Fragment inFragment, Fragment outFragment, boolean isPop, ArrayMap<String, View> sharedElements, boolean isStart) {
        SharedElementCallback sharedElementCallback;
        if (isPop) {
            sharedElementCallback = outFragment.getEnterTransitionCallback();
        } else {
            sharedElementCallback = inFragment.getEnterTransitionCallback();
        }
        if (sharedElementCallback != null) {
            ArrayList<View> views = new ArrayList<>();
            ArrayList<String> names = new ArrayList<>();
            int count = sharedElements == null ? 0 : sharedElements.size();
            for (int i = 0; i < count; i++) {
                names.add(sharedElements.keyAt(i));
                views.add(sharedElements.valueAt(i));
            }
            if (isStart) {
                sharedElementCallback.onSharedElementStart(names, views, null);
            } else {
                sharedElementCallback.onSharedElementEnd(names, views, null);
            }
        }
    }

    /* access modifiers changed from: private */
    public static ArrayList<View> configureEnteringExitingViews(Object transition, Fragment fragment, ArrayList<View> sharedElements, View nonExistentView) {
        ArrayList<View> viewList = null;
        if (transition != null) {
            viewList = new ArrayList<>();
            View root = fragment.getView();
            if (root != null) {
                FragmentTransitionCompat21.captureTransitioningViews(viewList, root);
            }
            if (sharedElements != null) {
                viewList.removeAll(sharedElements);
            }
            if (!viewList.isEmpty()) {
                viewList.add(nonExistentView);
                FragmentTransitionCompat21.addTargets(transition, viewList);
            }
        }
        return viewList;
    }

    /* access modifiers changed from: private */
    public static void setViewVisibility(ArrayList<View> views, int visibility) {
        if (views != null) {
            for (int i = views.size() - 1; i >= 0; i--) {
                views.get(i).setVisibility(visibility);
            }
        }
    }

    private static Object mergeTransitions(Object enterTransition, Object exitTransition, Object sharedElementTransition, Fragment inFragment, boolean isPop) {
        boolean z;
        boolean overlap = true;
        if (!(enterTransition == null || exitTransition == null || inFragment == null)) {
            if (isPop) {
                z = inFragment.getAllowReturnTransitionOverlap();
            } else {
                z = inFragment.getAllowEnterTransitionOverlap();
            }
            overlap = z;
        }
        if (overlap) {
            return FragmentTransitionCompat21.mergeTransitionsTogether(exitTransition, enterTransition, sharedElementTransition);
        }
        return FragmentTransitionCompat21.mergeTransitionsInSequence(exitTransition, enterTransition, sharedElementTransition);
    }

    public static void calculateFragments(BackStackRecord transaction, SparseArray<FragmentContainerTransition> transitioningFragments, boolean isReordered) {
        int numOps = transaction.mOps.size();
        for (int opNum = 0; opNum < numOps; opNum++) {
            addToFirstInLastOut(transaction, transaction.mOps.get(opNum), transitioningFragments, false, isReordered);
        }
    }

    public static void calculatePopFragments(BackStackRecord transaction, SparseArray<FragmentContainerTransition> transitioningFragments, boolean isReordered) {
        if (transaction.mManager.mContainer.onHasView()) {
            for (int opNum = transaction.mOps.size() - 1; opNum >= 0; opNum--) {
                addToFirstInLastOut(transaction, transaction.mOps.get(opNum), transitioningFragments, true, isReordered);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:86:0x00f2  */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x0106  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void addToFirstInLastOut(com.miui.systemui.support.v4.app.BackStackRecord r23, com.miui.systemui.support.v4.app.BackStackRecord.Op r24, android.util.SparseArray<com.miui.systemui.support.v4.app.FragmentTransition.FragmentContainerTransition> r25, boolean r26, boolean r27) {
        /*
            r0 = r23
            r1 = r24
            r2 = r25
            r3 = r26
            com.miui.systemui.support.v4.app.Fragment r10 = r1.fragment
            if (r10 != 0) goto L_0x000d
            return
        L_0x000d:
            int r11 = r10.mContainerId
            if (r11 != 0) goto L_0x0012
            return
        L_0x0012:
            if (r3 == 0) goto L_0x001b
            int[] r4 = INVERSE_OPS
            int r5 = r1.cmd
            r4 = r4[r5]
            goto L_0x001d
        L_0x001b:
            int r4 = r1.cmd
        L_0x001d:
            r12 = r4
            r4 = 0
            r5 = 0
            r6 = 0
            r7 = 0
            r8 = 0
            r9 = 1
            if (r12 == r9) goto L_0x008f
            switch(r12) {
                case 3: goto L_0x0065;
                case 4: goto L_0x0046;
                case 5: goto L_0x0030;
                case 6: goto L_0x0065;
                case 7: goto L_0x008f;
                default: goto L_0x0029;
            }
        L_0x0029:
            r13 = r4
            r15 = r5
            r16 = r6
            r14 = r7
            goto L_0x00a1
        L_0x0030:
            if (r27 == 0) goto L_0x0042
            boolean r13 = r10.mHiddenChanged
            if (r13 == 0) goto L_0x0040
            boolean r13 = r10.mHidden
            if (r13 != 0) goto L_0x0040
            boolean r13 = r10.mAdded
            if (r13 == 0) goto L_0x0040
            r8 = r9
        L_0x0040:
            r4 = r8
            goto L_0x0044
        L_0x0042:
            boolean r4 = r10.mHidden
        L_0x0044:
            r7 = 1
            goto L_0x0029
        L_0x0046:
            if (r27 == 0) goto L_0x0058
            boolean r13 = r10.mHiddenChanged
            if (r13 == 0) goto L_0x0056
            boolean r13 = r10.mAdded
            if (r13 == 0) goto L_0x0056
            boolean r13 = r10.mHidden
            if (r13 == 0) goto L_0x0056
            r8 = r9
        L_0x0056:
            r6 = r8
            goto L_0x0063
        L_0x0058:
            boolean r13 = r10.mAdded
            if (r13 == 0) goto L_0x0062
            boolean r13 = r10.mHidden
            if (r13 != 0) goto L_0x0062
            r8 = r9
        L_0x0062:
            r6 = r8
        L_0x0063:
            r5 = 1
            goto L_0x0029
        L_0x0065:
            if (r27 == 0) goto L_0x0082
            boolean r13 = r10.mAdded
            if (r13 != 0) goto L_0x0080
            android.view.View r13 = r10.mView
            if (r13 == 0) goto L_0x0080
            android.view.View r13 = r10.mView
            int r13 = r13.getVisibility()
            if (r13 != 0) goto L_0x0080
            float r13 = r10.mPostponedAlpha
            r14 = 0
            int r13 = (r13 > r14 ? 1 : (r13 == r14 ? 0 : -1))
            if (r13 < 0) goto L_0x0080
            r8 = r9
        L_0x0080:
            r6 = r8
            goto L_0x008d
        L_0x0082:
            boolean r13 = r10.mAdded
            if (r13 == 0) goto L_0x008c
            boolean r13 = r10.mHidden
            if (r13 != 0) goto L_0x008c
            r8 = r9
        L_0x008c:
            r6 = r8
        L_0x008d:
            r5 = 1
            goto L_0x0029
        L_0x008f:
            if (r27 == 0) goto L_0x0094
            boolean r4 = r10.mIsNewlyAdded
            goto L_0x009f
        L_0x0094:
            boolean r13 = r10.mAdded
            if (r13 != 0) goto L_0x009e
            boolean r13 = r10.mHidden
            if (r13 != 0) goto L_0x009e
            r8 = r9
        L_0x009e:
            r4 = r8
        L_0x009f:
            r7 = 1
            goto L_0x0029
        L_0x00a1:
            java.lang.Object r4 = r2.get(r11)
            com.miui.systemui.support.v4.app.FragmentTransition$FragmentContainerTransition r4 = (com.miui.systemui.support.v4.app.FragmentTransition.FragmentContainerTransition) r4
            if (r13 == 0) goto L_0x00b4
            com.miui.systemui.support.v4.app.FragmentTransition$FragmentContainerTransition r4 = ensureContainer(r4, r2, r11)
            r4.lastIn = r10
            r4.lastInIsPop = r3
            r4.lastInTransaction = r0
        L_0x00b4:
            r8 = r4
            r7 = 0
            if (r27 != 0) goto L_0x00ed
            if (r14 == 0) goto L_0x00ed
            if (r8 == 0) goto L_0x00c2
            com.miui.systemui.support.v4.app.Fragment r4 = r8.firstOut
            if (r4 != r10) goto L_0x00c2
            r8.firstOut = r7
        L_0x00c2:
            com.miui.systemui.support.v4.app.FragmentManagerImpl r6 = r0.mManager
            int r4 = r10.mState
            if (r4 >= r9) goto L_0x00ed
            int r4 = r6.mCurState
            if (r4 < r9) goto L_0x00ed
            boolean r4 = r0.mReorderingAllowed
            if (r4 != 0) goto L_0x00ed
            r6.makeActive(r10)
            r9 = 1
            r17 = 0
            r18 = 0
            r19 = 0
            r4 = r6
            r5 = r10
            r20 = r6
            r6 = r9
            r9 = r7
            r7 = r17
            r21 = r8
            r8 = r18
            r1 = r9
            r9 = r19
            r4.moveToState(r5, r6, r7, r8, r9)
            goto L_0x00f0
        L_0x00ed:
            r1 = r7
            r21 = r8
        L_0x00f0:
            if (r16 == 0) goto L_0x0106
            r4 = r21
            if (r4 == 0) goto L_0x00fa
            com.miui.systemui.support.v4.app.Fragment r5 = r4.firstOut
            if (r5 != 0) goto L_0x0108
        L_0x00fa:
            com.miui.systemui.support.v4.app.FragmentTransition$FragmentContainerTransition r8 = ensureContainer(r4, r2, r11)
            r8.firstOut = r10
            r8.firstOutIsPop = r3
            r8.firstOutTransaction = r0
            goto L_0x0109
        L_0x0106:
            r4 = r21
        L_0x0108:
            r8 = r4
        L_0x0109:
            if (r27 != 0) goto L_0x0115
            if (r15 == 0) goto L_0x0115
            if (r8 == 0) goto L_0x0115
            com.miui.systemui.support.v4.app.Fragment r4 = r8.lastIn
            if (r4 != r10) goto L_0x0115
            r8.lastIn = r1
        L_0x0115:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.systemui.support.v4.app.FragmentTransition.addToFirstInLastOut(com.miui.systemui.support.v4.app.BackStackRecord, com.miui.systemui.support.v4.app.BackStackRecord$Op, android.util.SparseArray, boolean, boolean):void");
    }

    private static FragmentContainerTransition ensureContainer(FragmentContainerTransition containerTransition, SparseArray<FragmentContainerTransition> transitioningFragments, int containerId) {
        if (containerTransition != null) {
            return containerTransition;
        }
        FragmentContainerTransition containerTransition2 = new FragmentContainerTransition();
        transitioningFragments.put(containerId, containerTransition2);
        return containerTransition2;
    }
}
