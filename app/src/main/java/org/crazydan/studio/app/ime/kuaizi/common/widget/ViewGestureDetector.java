/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.common.widget;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.core.msg.Motion;

/**
 * 手势检测器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-13
 */
public class ViewGestureDetector {
    /** 确定长按的超时时间 */
    private static final long LONG_PRESS_TIMEOUT_MILLS = 200;
    /** 确定长按 tick 的超时时间 */
    private static final long LONG_PRESS_TICK_TIMEOUT_MILLS = 100;
    /** 确定双击的超时时间 */
    private static final long DOUBLE_TAP_TIMEOUT_MILLS = 300;
    /** 确定滑动的超时时间 */
    private static final long FLIPPING_TIMEOUT_MILLS = 400;

    protected final Logger log = Logger.getLogger(getClass());

    private final Set<Listener> listeners = new LinkedHashSet<>();
    private final List<GestureData> movingTracker = new ArrayList<>();

    private boolean moving;
    private boolean longPressing;
    private LongPressHandler longPressHandler;

    private GestureData latestPressStart;
    private SingleTapGestureData latestSingleTap;

    public interface Listener {
        void onGesture(GestureType type, GestureData data);
    }

    public ViewGestureDetector addListener(Listener listener) {
        this.listeners.add(listener);
        return this;
    }

    public void reset() {
        this.log.beginTreeLog("Gesture Reset");

        onGestureEnd(this.latestPressStart);

        this.log.endTreeLog();
    }

    public void onTouchEvent(@NonNull MotionEvent e) {
        GestureData data = GestureData.from(e);

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                // Note: 优先触发长按监听，以确保其在指定的延时后能够及时执行，
                // 而不会因为后续监听的执行导致其执行被延后
                startLongPress(data);
                onPressStart(data);
                break;
            }
            // Note: 有些机型会在 ACTION_DOWN 后立即触发 ACTION_MOVE
            case MotionEvent.ACTION_MOVE: {
                // Note: 移动开始时，可能还未触发长按监听，故，需显式取消长按监听
                if (!this.longPressing) {
                    stopLongPress();
                }

                onMoving(data);
                break;
            }
            case MotionEvent.ACTION_UP: {
                // Note: ACTION_UP 会触发多次，需确保仅与最近的 ACTION_DOWN 相邻的才有效
                if (this.latestPressStart != null) {
                    if (!this.longPressing && !this.moving) {
                        onSingleTap(data);
                    } else if (!this.longPressing && isFlipping()) {
                        onFlipping(data);
                    }
                }

                onGestureEnd(data);
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                onGestureEnd(data);
                break;
            }
        }
    }

    private void onGestureEnd(GestureData data) {
        // Note: 先结束带定时任务的事件
        onLongPressEnd(data);
        onMovingEnd(data);

        onPressEnd(data);
    }

    private void onPressStart(GestureData data) {
        this.latestPressStart = data;

        triggerListeners(GestureType.PressStart, data);
    }

    private void onPressEnd(GestureData data) {
        this.latestPressStart = null;
        // Note: 最近的单击数据不能被重置，其将用于判断多次点击是否为有效的连击，如双击等
        //this.latestSingleTap = null;

        if (data != null) {
            triggerListeners(GestureType.PressEnd, data);
        }
    }

    private void startLongPress(GestureData data) {
        stopLongPress();

        LongPressHandler handler = getLongPressHandler();
        Message msg = handler.obtainMessage(LongPressHandler.MSG_LONG_PRESS, data);

        handler.sendMessageDelayed(msg, LONG_PRESS_TIMEOUT_MILLS);
    }

    private void startLongPressTick(LongPressTickGestureData data) {
        if (!this.longPressing) {
            return;
        }

        long timeout = LONG_PRESS_TICK_TIMEOUT_MILLS;
        LongPressTickGestureData newData = new LongPressTickGestureData(data, data.tick + 1, data.duration + timeout);

        LongPressHandler handler = getLongPressHandler();
        Message msg = handler.obtainMessage(LongPressHandler.MSG_LONG_PRESS_TICK, newData);

        handler.sendMessageDelayed(msg, timeout);
    }

    private void stopLongPress() {
        this.longPressing = false;

        if (this.longPressHandler != null) {
            this.longPressHandler.stop();
        }
    }

    private void onLongPressStart(GestureData data) {
        this.longPressing = true;

        GestureData newData = GestureData.newFrom(data);
        triggerListeners(GestureType.LongPressStart, newData);

        // 事件处理完后，再准备首次触发 tick
        LongPressTickGestureData tickData = new LongPressTickGestureData(data, 0, 0);
        startLongPressTick(tickData);
    }

    private void onLongPressTick(LongPressTickGestureData data) {
        if (!this.longPressing) {
            return;
        }

        // 若发生了移动，则需要更新 tick 事件发生位置
        int size = this.movingTracker.size();
        if (size > 0) {
            GestureData g = this.movingTracker.get(size - 1);
            data = new LongPressTickGestureData(g, data.tick, data.duration);
        }

        // 分发当前 tick 事件
        LongPressTickGestureData newData = LongPressTickGestureData.newFrom(data);
        triggerListeners(GestureType.LongPressTick, newData);

        // 事件处理完后，再准备触发下一个 tick
        startLongPressTick(data);
    }

    private void onLongPressEnd(GestureData data) {
        boolean hasLongPressing = this.longPressing;
        stopLongPress();

        if (hasLongPressing && data != null) {
            triggerListeners(GestureType.LongPressEnd, data);
        }
    }

    private void onSingleTap(GestureData data) {
        SingleTapGestureData tapData = new SingleTapGestureData(data, 0);

        SingleTapGestureData latestTapData = this.latestSingleTap;
        boolean isContinuousTap = latestTapData != null //
                                  && data.timestamp - latestTapData.timestamp < DOUBLE_TAP_TIMEOUT_MILLS;

        if (isContinuousTap) {
            tapData = new SingleTapGestureData(data, latestTapData.tick + 1);
        }
        this.latestSingleTap = tapData;

        // Note：双击也会触发两次单击事件，且均先于双击事件触发
        triggerListeners(GestureType.SingleTap, tapData);

        // 仅连续单击中的第二次才触发双击事件
        if (tapData.tick == 1) {
            triggerListeners(GestureType.DoubleTap, data);
        }
    }

    private void onMoving(GestureData data) {
        // Note: PressStart、MovingStart、Flipping 均须发生在相同的位置上
        if (!this.moving && this.movingTracker.isEmpty()) {
            // Note: 在一次完整的 按下 到 释放 的过程中，可能发生 #reset() 重置的情况，
            // 这时，需忽略对移动手势的处理
            if (this.latestPressStart == null) {
                return;
            }
            data = this.latestPressStart;
        }

        // 跳过最后位置不变的轨迹点
        GestureData last = CollectionUtils.last(this.movingTracker);
        if (last != null && last.x == data.x && last.y == data.y) {
            return;
        }
        this.movingTracker.add(data);

        int size = this.movingTracker.size();
        if (size < 2) {
            return;
        }
        this.moving = true;

        GestureData g1 = this.movingTracker.get(1);
        GestureData g2 = this.movingTracker.get(size - 1);

        Motion motion = createMotion(g2, g1);
        GestureData newData = new MovingGestureData(data, motion);

        if (size == 2) {
            triggerListeners(GestureType.MovingStart, data);
        } else {
            triggerListeners(GestureType.Moving, newData);
        }
    }

    private void onMovingEnd(GestureData data) {
        boolean hasMoving = this.moving;
        this.moving = false;
        this.movingTracker.clear();

        if (hasMoving && data != null) {
            triggerListeners(GestureType.MovingEnd, data);
        }
    }

    private void onFlipping(GestureData data) {
        int size = this.movingTracker.size();
        GestureData g1 = this.movingTracker.get(0);
        // Note: g2 应该始终与 data 相同
        GestureData g2 = this.movingTracker.get(size - 1);

        Motion motion = createMotion(g2, g1);
        if (motion.distance <= 0) {
            return;
        }

        // Note: 坐标位置设置为事件初始发生位置
        GestureData newData = new FlippingGestureData(g1, motion);

        triggerListeners(GestureType.Flipping, newData);
    }

    private boolean isFlipping() {
        int size = this.movingTracker.size();
        if (size < 2) {
            return false;
        }

        GestureData g1 = this.movingTracker.get(0);
        GestureData g2 = this.movingTracker.get(size - 1);

        return g2.timestamp - g1.timestamp < FLIPPING_TIMEOUT_MILLS;
    }

    private void triggerListeners(GestureType type, GestureData data) {
        for (Listener listener : this.listeners) {
            this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] { type, listener.getClass() });

            listener.onGesture(type, data);

            this.log.endTreeLog();
        }
    }

    private Motion createMotion(GestureData newData, GestureData oldData) {
        if (oldData == null) {
            return new Motion(Motion.Direction.none, 0, newData.timestamp);
        }

        long timestamp = newData.timestamp;

        double dx = newData.x - oldData.x;
        double dy = newData.y - oldData.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.toDegrees(Math.acos(dx / distance));

        Motion.Direction direction;
        // Note: 屏幕绘图坐标与空间坐标存在上下翻转关系
        //  ----- x
        //  |
        //  |
        //  y
        if (angle >= 45 && angle < 45 + 90) {
            direction = dy > 0 ? Motion.Direction.down : Motion.Direction.up;
        } else if (angle >= 45 + 90 && angle <= 180) {
            direction = Motion.Direction.left;
        } else {
            direction = Motion.Direction.right;
        }

        return new Motion(direction, (int) distance, timestamp);
    }

    private LongPressHandler getLongPressHandler() {
        if (this.longPressHandler == null) {
            this.longPressHandler = new LongPressHandler(this);
        }
        return this.longPressHandler;
    }

    protected String getActionName(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                return "ACTION_DOWN";
            }
            case MotionEvent.ACTION_MOVE: {
                return "ACTION_MOVE";
            }
            case MotionEvent.ACTION_UP: {
                return "ACTION_UP";
            }
            case MotionEvent.ACTION_CANCEL: {
                return "ACTION_CANCEL";
            }
        }
        return null;
    }

    public enum GestureType {
        /** 开始按压 */
        PressStart,
        /** 结束按压 */
        PressEnd,
        /** 开始长按 */
        LongPressStart,
        /** 长按 tick */
        LongPressTick,
        /** 结束长按 */
        LongPressEnd,
        /** 单击 */
        SingleTap,
        /** 双击 */
        DoubleTap,
        /** 开始移动 */
        MovingStart,
        /** 移动: 手指在屏幕上移动 */
        Moving,
        /** 结束移动 */
        MovingEnd,
        /** 翻动: 在一段时间内完成手指按下、移动到抬起的过程，期间没有其他动作 */
        Flipping,
    }

    public static class GestureData {
        public final float x;
        public final float y;
        public final long timestamp;

        public GestureData(float x, float y, long timestamp) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "{x=" + this.x + ", y=" + this.y + '}';
        }

        public static GestureData newFrom(GestureData g, float x, float y) {
            return new GestureData(x, y, g.timestamp);
        }

        /** 事件位置不变，仅修改时间戳为当前时间 */
        public static GestureData newFrom(GestureData g) {
            return new GestureData(g.x, g.y, SystemClock.uptimeMillis());
        }

        public static GestureData from(MotionEvent e) {
            return new GestureData(e.getX(), e.getY(), e.getEventTime());
        }
    }

    public static class MovingGestureData extends GestureData {
        public final Motion motion;

        public MovingGestureData(GestureData g, Motion motion) {
            super(g.x, g.y, g.timestamp);
            this.motion = motion;
        }

        @Override
        public String toString() {
            return "{motion=" + this.motion + '}';
        }
    }

    public static class FlippingGestureData extends GestureData {
        public final Motion motion;

        public FlippingGestureData(GestureData g, Motion motion) {
            super(g.x, g.y, g.timestamp);
            this.motion = motion;
        }

        @Override
        public String toString() {
            return "{motion=" + this.motion + '}';
        }
    }

    public static class TickGestureData extends GestureData {
        public final int tick;

        public TickGestureData(GestureData g, int tick) {
            super(g.x, g.y, g.timestamp);
            this.tick = tick;
        }

        @Override
        public String toString() {
            return "{tick=" + this.tick + '}';
        }
    }

    public static class LongPressTickGestureData extends TickGestureData {
        public final long duration;

        public LongPressTickGestureData(GestureData g, int tick, long duration) {
            super(g, tick);
            this.duration = duration;
        }

        /** 事件位置、tick、duration 不变，仅修改时间戳为当前时间 */
        public static LongPressTickGestureData newFrom(LongPressTickGestureData g) {
            return new LongPressTickGestureData(GestureData.newFrom(g), g.tick, g.duration);
        }
    }

    public static class SingleTapGestureData extends TickGestureData {

        public SingleTapGestureData(GestureData g, int tick) {
            super(g, tick);
        }
    }

    private static class LongPressHandler extends Handler {
        private static final int MSG_LONG_PRESS = 1;
        private static final int MSG_LONG_PRESS_TICK = 2;

        private final ViewGestureDetector detector;

        public LongPressHandler(ViewGestureDetector detector) {
            super(Looper.getMainLooper());

            this.detector = detector;
        }

        public void stop() {
            removeMessages(MSG_LONG_PRESS_TICK);
            removeMessages(MSG_LONG_PRESS);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LONG_PRESS:
                    this.detector.onLongPressStart((GestureData) msg.obj);
                    break;
                case MSG_LONG_PRESS_TICK:
                    this.detector.onLongPressTick((LongPressTickGestureData) msg.obj);
                    break;
            }
        }
    }
}
