/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2023 Crazydan Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.crazydan.studio.app.ime.kuaizi.pane.keyboard;

import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.conf.Conf;
import org.crazydan.studio.app.ime.kuaizi.conf.Configuration;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.EditorEditAction;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.EditorCursorMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputAudioPlayMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputCharsInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputCharsInputPopupShowMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputListCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputListPairSymbolCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.KeyboardHandModeSwitchMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.KeyboardStateChangeMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.KeyboardSwitchMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.user.UserFingerFlippingMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.user.UserSingleTapMsgData;

import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Editor_Cursor_Move_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Editor_Edit_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.IME_Switch_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputAudio_Play_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputChars_Input_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputChars_Input_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputChars_Input_Popup_Hide_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputChars_Input_Popup_Show_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputList_Commit_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputList_Committed_Revoke_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputList_PairSymbol_Commit_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Input_Choose_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Input_Completion_Update_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Input_Pending_Drop_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Input_Selected_Delete_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_HandMode_Switch_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_State_Change_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_Switch_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_XPad_Simulation_Terminated;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public abstract class BaseKeyboard implements Keyboard {
    private InputMsgListener listener;

    protected State state = new State(State.Type.InputChars_Input_Wait_Doing);

    @Override
    public void setListener(InputMsgListener listener) {
        this.listener = listener;
    }

    /** 获取键盘初始状态，即，{@link State.Type#InputChars_Input_Wait_Doing 待输入}状态 */
    protected State getInitState() {
        if (this.state.type == State.Type.InputChars_Input_Wait_Doing) {
            return createInitState();
        }
        return new State(State.Type.InputChars_Input_Wait_Doing, this.state);
    }

    protected State createInitState() {
        return new State(State.Type.InputChars_Input_Wait_Doing);
    }

    public boolean isXInputPadEnabled() {
        return getConfig().isXInputPadEnabled();
    }

    @Override
    public void start(InputList inputList) {
        Input<?> pending = inputList.getPending();
        boolean isXPadSwitchToPinyin = isXInputPadEnabled() //
                                       && this.prevType != null //
                                       && getType() == Type.Pinyin;
        // 在 X 型输入中，切换到拼音键盘时，先确认新输入（非新输入将做输入替换）
        if (isXPadSwitchToPinyin && inputList.isGapSelected()) {
            inputList.confirmPendingAndSelectNext();
        }

        // 将算术键盘视为内嵌键盘，故而，在选中其他类型输入时，需做选择处理。
        // 而对于其他键盘（非 X 型输入），选中的输入将视为将被替换的输入，故不做选择处理
        if ((this.prevType == Type.Math //
             && !pending.isMathExpr()) //
            || (isXPadSwitchToPinyin && pending.isPinyin()) //
        ) {
            choose_InputList_Selected_Input(inputList);
        }
    }

    @Override
    public void reset() {
        change_State_to_Init();
    }

    @Override
    public void destroy() {
    }

    protected KeyTable.Config createKeyTableConfig(InputList inputList) {
        return new KeyTable.Config(getConfig(),
                                   !inputList.isEmpty(),
                                   inputList.canRevokeCommit(),
                                   !inputList.isGapSelected());
    }

    @Override
    public void onMsg(InputList inputList, InputMsg msg) {
        switch (msg.type) {
            case Input_Choose_Doing: {
                switch (this.state.type) {
                    case InputChars_Input_Wait_Doing: {
                        choose_InputList_Input(inputList, msg.data.input);
                        break;
                    }
                }
                break;
            }
        }
    }

    // ======================== Start: 对 UserKeyMsg 的通用处理 ========================

    /**
     * 尝试对 {@link UserKeyMsg} 做处理
     * <p/>
     * {@link CtrlKey#isDisabled() 被禁用}的 {@link CtrlKey} 将始终返回 <code>true</code>
     *
     * @return 若返回 true，则表示消息已处理，否则，返回 false
     */
    protected boolean try_On_Common_UserKey_Msg(InputList inputList, UserKeyMsg msg) {
        Key<?> key = msg.data.key;

        // Note: NoOp 控制按键上的消息不能忽略，滑屏输入和翻页等状态下会涉及该类控制按键的消息处理
        if (key instanceof CtrlKey //
            && (key.isDisabled() //
                || try_On_Common_CtrlKey_Msg(inputList, msg, (CtrlKey) key))) {
            return true;
        }

        return try_On_UserKey_Msg_Over_XPad(msg, key);
    }

    /** 尝试处理控制按键消息 */
    protected boolean try_On_Common_CtrlKey_Msg(InputList inputList, UserKeyMsg msg, CtrlKey key) {
        switch (msg.type) {
            case LongPress_Key_Tick: {
                switch (key.getType()) {
                    case Backspace:
                    case Space:
                    case Enter:
                        // 长按 tick 视为连续单击
                        return try_On_Common_CtrlKey_Msg(inputList,
                                                         new UserKeyMsg(UserKeyMsgType.SingleTap_Key, msg.data),
                                                         key);
                }
                break;
            }
            case SingleTap_Key: {
                switch (key.getType()) {
                    // Note：在任意子键盘中提交输入，都需直接回到初始键盘
                    case Commit_InputList: {
                        play_SingleTick_InputAudio(key);
                        commit_InputList_and_Goto_Init_State(inputList);
                        return true;
                    }
                    case DropInput: {
                        switch (this.state.type) {
                            case Emoji_Choose_Doing:
                            case Symbol_Choose_Doing:
                                play_SingleTick_InputAudio(key);
                                delete_InputList_Selected(inputList, key);
                                return true;
                        }
                        break;
                    }
                    case RevokeInput: {
                        play_SingleTick_InputAudio(key);
                        revoke_Committed_InputList(inputList, key);
                        return true;
                    }
                    case Backspace: {
                        play_SingleTick_InputAudio(key);
                        show_InputChars_Input_Popup(key);

                        backspace_InputList_or_Editor(inputList, key);
                        return true;
                    }
                    case Space:
                    case Enter: {
                        play_SingleTick_InputAudio(key);
                        show_InputChars_Input_Popup(key);

                        confirm_InputList_Input_Enter_or_Space(inputList, key);
                        return true;
                    }
                    case Exit: {
                        play_SingleTick_InputAudio(key);
                        exit_Keyboard(key);
                        return true;
                    }
                    case Switch_IME: {
                        play_SingleTick_InputAudio(key);
                        switch_IME(key);
                        return true;
                    }
                    case Switch_HandMode: {
                        play_SingleTick_InputAudio(key);
                        switch_HandMode(key);
                        return true;
                    }
                    case Switch_Keyboard: {
                        play_SingleTick_InputAudio(key);

                        CtrlKey.KeyboardSwitchOption option = (CtrlKey.KeyboardSwitchOption) key.getOption();
                        switch_Keyboard_To(option.value(), key);
                        return true;
                    }
                }
                break;
            }
        }

        // 处理定位按钮
        if (CtrlKey.is(key, CtrlKey.Type.Editor_Cursor_Locator)) {
            switch (msg.type) {
                case SingleTap_Key: {
                    // 为双击提前播放音效
                    play_SingleTick_InputAudio(key);
                    return true;
                }
                case LongPress_Key_Start: {
                    play_DoubleTick_InputAudio(key);
                    // 继续 DoubleTap_Key 的逻辑
                }
                case DoubleTap_Key: {
                    switch_Keyboard_To(Type.Editor, key);
                    return true;
                }
                case FingerFlipping: {
                    play_SingleTick_InputAudio(key);

                    // 在定位切换按钮上滑动也可以移动光标，但不修改键盘状态
                    Motion motion = ((UserFingerFlippingMsgData) msg.data).motion;

                    do_Editor_Cursor_Moving(key, motion);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean try_On_UserKey_Msg_Over_XPad(UserKeyMsg msg, Key<?> key) {
        if (!isXInputPadEnabled() || !(key instanceof CtrlKey)) {
            return false;
        }

        switch (msg.type) {
            case Press_Key_Stop: {
                if (CtrlKey.is(key, CtrlKey.Type.XPad_Simulation_Terminated)) {
                    fire_InputMsg(Keyboard_XPad_Simulation_Terminated, new InputMsgData());
                    return true;
                } else {
                    break;
                }
            }
            case FingerMoving: {
                // 播放输入分区激活和待输入按键切换的提示音
                switch (((CtrlKey) key).getType()) {
                    case XPad_Active_Block: {
                        play_PingTick_InputAudio(key);
                        return true;
                    }
                    case XPad_Char_Key: {
                        play_ClockTick_InputAudio(key);
                        return true;
                    }
                }
                break;
            }
        }

        return false;
    }

    // ======================== End: 对 UserKeyMsg 的通用处理 ========================

    // ====================== Start: 触发 InputMsg 消息 ======================

    /** 触发 {@link InputMsg} 消息 */
    protected void fire_InputMsg(InputMsgType msgType, InputMsgData msgData) {
        InputMsg msg = new InputMsg(msgType, msgData);
        this.listener.onMsg(msg);
    }

    protected void fire_Common_InputMsg(InputMsgType msgType, Key<?> key) {
        fire_Common_InputMsg(msgType, key, null);
    }

    protected void fire_Common_InputMsg(InputMsgType msgType, Input<?> input) {
        fire_Common_InputMsg(msgType, null, input);
    }

    protected void fire_Common_InputMsg(InputMsgType msgType, Key<?> key, Input<?> input) {
        InputMsgData data = new InputMsgData(key, input);
        fire_InputMsg(msgType, data);
    }

    /** 触发 {@link InputMsgType#InputChars_Input_Doing} 消息 */
    protected void fire_InputChars_Input_Doing_in_TapMode(Key<?> key, Input<?> input) {
        fire_InputChars_Input_Doing(key, input, InputCharsInputMsgData.InputMode.tap);
    }

    /** 触发 {@link InputMsgType#InputChars_Input_Doing} 消息 */
    protected void fire_InputChars_Input_Doing(Key<?> key, Input<?> input, InputCharsInputMsgData.InputMode inputMode) {
        InputMsgData data = new InputCharsInputMsgData(key, input, inputMode);

        fire_InputMsg(InputChars_Input_Doing, data);
    }

    /** 触发 {@link InputMsgType#InputChars_Input_Done} 消息 */
    protected void fire_InputChars_Input_Done(Key<?> key, Input<?> input) {
        InputMsgData data = new InputCharsInputMsgData(key, input);

        fire_InputMsg(InputChars_Input_Done, data);
    }

    /** 触发 {@link InputMsgType#InputList_Commit_Doing} 消息 */
    protected void fire_InputList_Commit_Doing(CharSequence text, List<String> replacements) {
        InputMsgData data = new InputListCommitMsgData(text, replacements);

        fire_InputMsg(InputList_Commit_Doing, data);
    }

    /** 触发 {@link InputMsgType#InputList_PairSymbol_Commit_Doing} 消息 */
    protected void fire_InputList_PairSymbol_Commit_Doing(CharSequence left, CharSequence right) {
        InputMsgData data = new InputListPairSymbolCommitMsgData(left, right);

        fire_InputMsg(InputList_PairSymbol_Commit_Doing, data);
    }

    /** 触发 {@link InputMsgType#InputAudio_Play_Doing} 消息 */
    private void fire_InputAudio_Play_Doing(Key<?> key, InputAudioPlayMsgData.AudioType audioType) {
        if (CtrlKey.isNoOp(key)) {
            return;
        }

        InputMsgData data = new InputAudioPlayMsgData(key, audioType);
        fire_InputMsg(InputAudio_Play_Doing, data);
    }

    // ====================== End: 触发 InputMsg 消息 ======================

    /** 状态回到{@link State.Type#InputChars_Input_Wait_Doing 待输入} */
    protected void change_State_to_Init() {
        change_State_to_Init(null);
    }

    /**
     * 状态回到{@link State.Type#InputChars_Input_Wait_Doing 待输入}
     *
     * @param key
     *         触发状态变化的按键
     */
    protected void change_State_to_Init(Key<?> key) {
        change_State_To(getInitState(), key);
    }

    protected void change_State_To(State state, Key<?> key) {
        this.state = state;

        InputMsgData data = new KeyboardStateChangeMsgData(key, state);
        fire_InputMsg(Keyboard_State_Change_Done, data);
    }

    /**
     * 回到前序状态
     * <p/>
     * 若无前序状态，则回到初始状态
     */
    protected void change_State_to_Previous(Key<?> key) {
        State state = this.state;
        State previous = state.previous;

        // 跳过与当前状态的类型相同的前序状态
        while (previous != null && previous.type == state.type) {
            state = previous;
            previous = state.previous;
        }

        if (previous == null) {
            change_State_to_Init(key);
        } else {
            change_State_To(key, previous);
        }
    }

    /** 切换到指定类型的键盘 */
    protected void switch_Keyboard_To(Type type) {
        switch_Keyboard_To(type, null);
    }

    /** 切换到指定类型的键盘 */
    protected void switch_Keyboard_To(Type type, Key<?> key) {
        InputMsgData data = new KeyboardSwitchMsgData(key, type);

        fire_InputMsg(Keyboard_Switch_Doing, data);
    }

    /** 切换到先前的键盘，也就是从哪个键盘切过来的，就切回到哪个键盘 */
    protected void switch_Keyboard_to_Previous(Key<?> key) {
        switch_Keyboard_To(null, key);
    }

    /** 退出并返回到原状态或前序键盘 */
    protected void exit_Keyboard(Key<?> key) {
        if (this.state.previous == null) {
            switch_Keyboard_to_Previous(key);
        } else {
            change_State_to_Previous(key);
        }
    }

    /** 切换键盘的左右手模式 */
    protected void switch_HandMode(Key<?> key) {
        Configuration config = getConfig();
        HandMode mode = config.get(Conf.hand_mode);

        switch (mode) {
            case left:
                mode = Keyboard.HandMode.right;
                break;
            case right:
                mode = Keyboard.HandMode.left;
                break;
        }

        InputMsgData data = new KeyboardHandModeSwitchMsgData(key, mode);
        fire_InputMsg(Keyboard_HandMode_Switch_Doing, data);
    }

    /** 切换系统输入法 */
    protected void switch_IME(Key<?> key) {
        // Note：有可能切换，也有可能不切换，
        // 若发生切换，则再回来时键盘状态会主动被重置，
        // 故不需要提前重置键盘状态
        fire_Common_InputMsg(IME_Switch_Doing, key);
    }

    /** 显示输入提示气泡 */
    protected void show_InputChars_Input_Popup(Key<?> key) {
        show_InputChars_Input_Popup(key, true);
    }

    /**
     * 显示输入提示气泡
     *
     * @param hideDelayed
     *         是否延迟隐藏
     */
    protected void show_InputChars_Input_Popup(Key<?> key, boolean hideDelayed) {
        String text = key != null ? key.getLabel() : null;
        InputMsgData data = new InputCharsInputPopupShowMsgData(text, hideDelayed);

        fire_InputMsg(InputChars_Input_Popup_Show_Doing, data);
    }

    /** 隐藏输入提示气泡 */
    protected void hide_InputChars_Input_Popup() {
        InputMsgData data = new InputMsgData();

        fire_InputMsg(InputChars_Input_Popup_Hide_Doing, data);
    }

    /** 播放输入单击音效 */
    protected void play_SingleTick_InputAudio(Key<?> key) {
        fire_InputAudio_Play_Doing(key, InputAudioPlayMsgData.AudioType.SingleTick);
    }

    /** 播放输入双击音效 */
    protected void play_DoubleTick_InputAudio(Key<?> key) {
        fire_InputAudio_Play_Doing(key, InputAudioPlayMsgData.AudioType.DoubleTick);
    }

    /** 播放时钟走时音效 */
    protected void play_ClockTick_InputAudio(Key<?> key) {
        fire_InputAudio_Play_Doing(key, InputAudioPlayMsgData.AudioType.ClockTick);
    }

    /** 播放敲击音效 */
    protected void play_PingTick_InputAudio(Key<?> key) {
        fire_InputAudio_Play_Doing(key, InputAudioPlayMsgData.AudioType.PingTick);
    }

    /** 播放输入翻页音效 */
    protected void play_PageFlip_InputAudio() {
        fire_InputAudio_Play_Doing(null, InputAudioPlayMsgData.AudioType.PageFlip);
    }

    // ======================== Start: 文本编辑 ========================

    /** 回删输入列表中的输入内容 */
    protected void do_InputList_Backspacing(InputList inputList, Key<?> key) {
        inputList.deleteBackward();
        fire_InputChars_Input_Doing_in_TapMode(key, null);

        do_InputList_Pending_Completion_Updating(inputList);

        do_InputList_Current_Phrase_Completion_Updating(inputList);
    }

    /** 回删 目标编辑器 的内容 */
    protected void do_Editor_Backspacing(InputList inputList) {
        do_Editor_Editing(inputList, EditorEditAction.backspace);
    }

    protected void do_Editor_Editing(InputList inputList, EditorEditAction action) {
        switch (action) {
            case noop:
            case copy:
                // 不影响输入撤回的操作，则无需清空待撤回输入数据
                break;
            default:
                inputList.clearCommitRevokes();
        }

        InputMsgData data = new EditorEditMsgData(action);

        fire_InputMsg(Editor_Edit_Doing, data);
    }

    protected void do_Editor_Cursor_Moving(CtrlKey key, Motion motion) {
        InputMsgData data = new EditorCursorMsgData(key, motion);

        fire_InputMsg(Editor_Cursor_Move_Doing, data);
    }

    // ======================== End: 文本编辑逻辑 ========================

    // ======================== Start: 单字符输入 ========================

    /**
     * 单字符输入处理
     * <p/>
     * 对于有替代字符的按键，根据连续点击次数确定替代字符并替换前序按键字符
     */
    protected void start_Single_Key_Inputting(
            InputList inputList, Key<?> key, UserSingleTapMsgData data, boolean directInputting
    ) {
        play_SingleTick_InputAudio(key);
        show_InputChars_Input_Popup(key);

        inputList.clearPhraseCompletions();

        if (key instanceof CharKey && ((CharKey) key).hasReplacement() && data.tick > 0) {
            do_Single_CharKey_Replacement_Inputting(inputList, (CharKey) key, data.tick, directInputting);
        } else {
            do_Single_Key_Inputting(inputList, key, directInputting);
        }
    }

    /** 单一按键输入 */
    protected void do_Single_Key_Inputting(InputList inputList, Key<?> key, boolean directInputting) {
        if (directInputting) {
            commit_InputList_with_SingleKey_Only(inputList, key, false);
            return;
        }

        // Note：该类键盘不涉及配对符号的输入，故始终清空配对符号的绑定
        inputList.clearPairOnSelected();

        if (key instanceof CharKey) {
            do_Single_CharKey_Inputting(inputList, (CharKey) key);
        }
        // 针对非字符类型的表情和符号按键
        else if (key.isEmoji() || key.isSymbol()) {
            confirm_or_New_InputList_Pending(inputList);
            confirm_InputList_Input_with_SingleKey_Only(inputList, key);
        }
    }

    /** 单字符按键输入 */
    protected void do_Single_CharKey_Inputting(InputList inputList, CharKey key) {
        switch (key.getType()) {
            // 若为标点、表情符号，则直接确认输入，不支持连续输入其他字符
            case Emoji:
            case Symbol: {
                boolean directInputting = inputList.isEmpty();

                if (!directInputting) {
                    // Note：被选中的输入直接对其做替换
                    inputList.newPending();

                    inputList.getPending().appendKey(key);

                    confirm_InputList_Pending(inputList, key);
                } else {
                    // 单个标点、表情，直接提交输入
                    commit_InputList_with_SingleKey_Only(inputList, key, false);
                }
                break;
            }
            // 字母、数字可连续输入
            case Number:
            case Alphabet: {
                CharInput pending = inputList.getPending();
                // Note：非拉丁字符输入不可连续输入，直接对其做替换
                if (!pending.isLatin()) {
                    pending = inputList.newPending();
                }

                pending.appendKey(key);
                fire_InputChars_Input_Doing_in_TapMode(key, pending);

                do_InputList_Pending_Completion_Updating(inputList);
                break;
            }
        }
    }

    /** 处理单字符按键的 替换输入 */
    protected void do_Single_CharKey_Replacement_Inputting(
            InputList inputList, CharKey key, int replacementIndex, boolean directInputting
    ) {
        if (directInputting) {
            do_Single_CharKey_Replacement_Committing(inputList, key, replacementIndex);
            return;
        }

        Input<?> input;
        if (key.isSymbol()) {
            // Note：标点符号是独立输入，故，需替换当前位置的前一个标点符号输入（当前输入必然为 Gap）
            input = inputList.getInputBeforeSelected();

            // 对输入列表为空时的标点符号直输输入进行替换
            if (input == null) {
                do_Single_CharKey_Replacement_Committing(inputList, key, replacementIndex);
                return;
            }
        } else {
            input = inputList.getPending();
        }

        Key<?> lastKey = input.getLastKey();
        if (!key.canReplaceTheKey(lastKey)) {
            // 转为单字符输入
            do_Single_Key_Inputting(inputList, key, false);
            return;
        }

        CharKey lastCharKey = (CharKey) lastKey;
        // Note: 在 Input 中的按键可能不携带 replacement 信息，只能通过当前按键做判断
        String newKeyText = key.nextReplacement(lastCharKey.getText());
        CharKey newKey = key.createReplacementKey(newKeyText);

        input.replaceLatestKey(lastCharKey, newKey);

        show_InputChars_Input_Popup(newKey);
        fire_InputChars_Input_Doing_in_TapMode(newKey, input);
    }

    /** 提交单字符按键的替换输入：对编辑框内的输入字符做输入替换 */
    protected void do_Single_CharKey_Replacement_Committing(InputList inputList, CharKey key, int replacementIndex) {
        CharKey newKey = key.createReplacementKey(replacementIndex);
        show_InputChars_Input_Popup(newKey);

        commit_InputList_with_SingleKey_Only(inputList, newKey, true);
    }

    // ======================== End: 单字符输入 ========================

    // ======================== Start: 输入补全 ========================

    /** 查找以指定 text 开头的最靠前的 top 个拉丁文 */
    protected List<String> getTopBestMatchedLatins(String text) {
        return List.of();
    }

    /** 更新待输入的输入补全 */
    protected void do_InputList_Pending_Completion_Updating(InputList inputList) {
        CharInput pending = inputList.getPending();
        if (Input.isEmpty(pending) || !pending.isLatin()) {
            return;
        }
        pending.clearCompletions();

        String text = pending.getText().toString();
        getTopBestMatchedLatins(text).forEach((latin) -> {
            List<Key<?>> keys = CharKey.from(latin);

            if (!keys.isEmpty()) {
                CharInput input = CharInput.from(keys);
                CompletionInput completion = new CompletionInput(text.length());
                completion.add(input);

                pending.addCompletion(completion);
            }
        });

        fire_Common_InputMsg(Input_Completion_Update_Done, pending);
    }

    /** 更新当前输入位置的短语输入补全 */
    protected void do_InputList_Current_Phrase_Completion_Updating(InputList inputList) {
        inputList.clearPhraseCompletions();

        Input<?> input = inputList.getSelected();
        do_InputList_Phrase_Completion_Updating(inputList, input);

        fire_Common_InputMsg(Input_Completion_Update_Done, input);
    }

    protected void do_InputList_Phrase_Completion_Updating(InputList inputList, Input<?> input) {}

    // ======================== End: 输入补全 ========================

    // ======================== Start: 操作输入列表 ========================

    /** 选中输入列表中的已选择输入，用于再次触发对输入的 {@link #choose_InputList_Input 选中处理} */
    protected void choose_InputList_Selected_Input(InputList inputList) {
        Input<?> selected = inputList.getSelected();

        choose_InputList_Input(inputList, selected);
    }

    /** 选中输入列表中的指定输入，一般切换到该输入所对应的 {@link Keyboard} 上 */
    protected void choose_InputList_Input(InputList inputList, Input<?> input) {
        inputList.select(input);

        do_InputList_Current_Phrase_Completion_Updating(inputList);

        // Note：输入过程中操作和处理的都是 pending
        CharInput pending = inputList.getPending();

        // 处理选中的输入需要切换到原键盘的情况
        if (pending.isMathExpr()) {
            switch_Keyboard_To(Type.Math);
            return;
        } else if (isXInputPadEnabled()) {
            // Note：在 X 型输入中，各类键盘是可直接相互切换的，不需要退出再进入，
            // 故而，在选中其输入时，也需要能够直接进入其输入选择状态
            if (pending.isPinyin() && getType() != Type.Pinyin) {
                switch_Keyboard_To(Type.Pinyin);
                return;
            }
        }

        if (pending.isEmoji()) {
            switch_Keyboard_To(Type.Emoji);
        } else if (pending.isSymbol()) {
            switch_Keyboard_To(Type.Emoji);
        } else if (pending.isPinyin()) {
            switch_Keyboard_To(Type.Pinyin_Candidates);
        } else {
            // 在选择输入时，对于新输入，需先确认其 pending
            if (input.isGap() && !pending.isEmpty()) {
                confirm_InputList_Pending(inputList, null);
            }
            change_State_to_Init();

            fire_Common_InputMsg(Input_Choose_Done, input);
        }
    }

    /** 确认待输入，并触发 {@link InputMsgType#InputChars_Input_Done} 消息 */
    protected void confirm_InputList_Pending(InputList inputList, Key<?> key) {
        CharInput pending = inputList.getPending();

        inputList.confirmPendingAndSelectNext();

        fire_InputChars_Input_Done(key, pending);

        do_InputList_Current_Phrase_Completion_Updating(inputList);
    }

    /**
     * 对于 Gap 输入先提交其待输入，并选中后继 Gap；
     * 而对于已存在的输入，则直接新建待输入以做替换输入
     */
    protected void confirm_or_New_InputList_Pending(InputList inputList) {
        // 对于新增输入，先做提交，再录入
        if (inputList.isGapSelected()) {
            inputList.confirmPendingAndSelectNext();
        }
        // 对于修改输入，则直接对其做替换
        else {
            inputList.newPending();
        }
    }

    /** 仅{@link #confirm_InputList_Pending 确认}只有唯一按键的输入 */
    protected void confirm_InputList_Input_with_SingleKey_Only(InputList inputList, Key<?> key) {
        inputList.newPending().appendKey(key);

        confirm_InputList_Pending(inputList, key);
    }

    /**
     * 确认回车或空格的控制按键输入
     * <p/>
     * 为回车时，直接提交当前输入或在 目标编辑器 中输入换行；
     * 为空格时，当输入列表为空时，直接向 目标编辑器 输入空格，
     * 否则，将空格附加到输入列表中
     */
    protected void confirm_InputList_Input_Enter_or_Space(InputList inputList, CtrlKey key) {
        boolean isDirectInputting = inputList.isEmpty();

        if (isDirectInputting) {
            switch (key.getType()) {
                case Enter:
                case Space:
                    // Note：直输回车和空格后，不再支持输入撤回
                    inputList.clearCommitRevokes();

                    fire_InputList_Commit_Doing(key.getText(), null);
                    break;
            }
        }
        // 输入列表不为空且按键为空格按键时，将其添加到输入列表中
        else if (CtrlKey.is(key, CtrlKey.Type.Space)) {
            // Note：空格不替换当前输入
            inputList.confirmPendingAndSelectNext();

            confirm_InputList_Input_with_SingleKey_Only(inputList, key);
        }
    }

    /** 删除已选中的输入 */
    protected void delete_InputList_Selected(InputList inputList, Key<?> key) {
        Input<?> input = inputList.getSelected();

        inputList.deleteSelected();
        fire_Common_InputMsg(Input_Selected_Delete_Done, key, input);

        do_InputList_Current_Phrase_Completion_Updating(inputList);
    }

    /** 删除待输入 */
    protected void drop_InputList_Pending(InputList inputList, Key<?> key) {
        CharInput pending = inputList.getPending();

        inputList.dropPending();
        fire_Common_InputMsg(Input_Pending_Drop_Done, key, pending);

        do_InputList_Current_Phrase_Completion_Updating(inputList);
    }

    /**
     * {@link #commit_InputList 提交输入列表（可撤销）}，
     * 并{@link #change_State_to_Init 进入初始状态}
     */
    protected void commit_InputList_and_Goto_Init_State(InputList inputList) {
        commit_InputList(inputList, true, false);

        change_State_to_Init();
    }

    /** 提交单一按键输入，且该提交不可被撤销 */
    protected void commit_InputList_with_SingleKey_Only(InputList inputList, Key<?> key, boolean needToBeReplaced) {
        inputList.newPending().appendKey(key);

        commit_InputList(inputList, false, needToBeReplaced);
    }

    /** 提交输入列表 */
    protected void commit_InputList(InputList inputList, boolean canBeRevoked, boolean needToBeReplaced) {
        commit_InputList(inputList, canBeRevoked, needToBeReplaced, false);
    }

    /** 在 {@link #commit_InputList} 之前需要做的事情 */
    protected void before_Commit_InputList(InputList inputList) {}

    /** 提交输入列表 */
    protected void commit_InputList(
            InputList inputList, boolean canBeRevoked, boolean needToBeReplaced, boolean isPairSymbol
    ) {
        inputList.confirmPending();
        if (inputList.isEmpty()) {
            return;
        }

        before_Commit_InputList(inputList);

        if (isPairSymbol) {
            CharInput left = inputList.getFirstCharInput();
            CharInput right = inputList.getLastCharInput();

            inputList.commit(canBeRevoked);

            fire_InputList_PairSymbol_Commit_Doing(left.getText(), right.getText());
        } else {
            List<String> replacements = null;
            if (needToBeReplaced) {
                CharInput input = inputList.getLastCharInput();
                CharKey key = (CharKey) input.getLastKey();

                replacements = key.getReplacements();
            }

            StringBuilder text = inputList.commit(canBeRevoked);

            fire_InputList_Commit_Doing(text, replacements);
        }
    }

    /** 撤回输入列表，且状态保持不变 */
    protected void revoke_Committed_InputList(InputList inputList, Key<?> key) {
        if (!inputList.canRevokeCommit()) {
            return;
        }

        inputList.revokeCommit();
        after_Revoke_Committed_InputList(inputList);

        fire_Common_InputMsg(InputList_Committed_Revoke_Doing, key);

        choose_InputList_Selected_Input(inputList);
    }

    /** 在 {@link #revoke_Committed_InputList} 之后需要做的事情 */
    protected void after_Revoke_Committed_InputList(InputList inputList) {}

    /**
     * 回删输入列表中的输入或 目标编辑器 的内容
     * <p/>
     * 输入列表不为空时，在输入列表中做删除，否则，在 目标编辑器 做删除
     */
    protected void backspace_InputList_or_Editor(InputList inputList, Key<?> key) {
        inputList.clearCommitRevokes();

        if (!inputList.isEmpty()) {
            do_InputList_Backspacing(inputList, key);
        } else {
            do_Editor_Backspacing(inputList);
        }
    }

    // ======================== End: 操作输入列表 ========================
}
