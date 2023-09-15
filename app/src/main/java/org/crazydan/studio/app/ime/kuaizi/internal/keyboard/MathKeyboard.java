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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard;

import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharMathExprInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.MathKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;

/**
 * {@link Keyboard.Type#Math 数学键盘}
 * <p/>
 * 含数字、计算符号等
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-04
 */
public class MathKeyboard extends BaseKeyboard {

    @Override
    protected KeyFactory doGetKeyFactory() {
        MathKeyTable keyTable = MathKeyTable.create(createKeyTableConfigure());

        return keyTable::createKeys;
    }

    @Override
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        if (msg == UserInputMsg.Cleaning_Inputs) {
            super.onUserInputMsg(msg, data);
        }
    }

    @Override
    public void onUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data) {
        if (try_OnUserKeyMsg(msg, data)) {
            return;
        }

        Key<?> key = data.target;
        if (key instanceof CharKey) {
            onCharKeyMsg(msg, (CharKey) key, data);
        } else if (key instanceof CtrlKey) {
            onCtrlKeyMsg(msg, (CtrlKey) key, data);
        }
    }

    private void onCharKeyMsg(UserKeyMsg msg, CharKey key, UserKeyMsgData data) {
        if (msg == UserKeyMsg.KeySingleTap) {// 单字符输入
            play_InputtingSingleTick_Audio(key);

            do_MathKey_Inputting(key);
        }
    }

    private void onCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        if (msg == UserKeyMsg.KeySingleTap) {
            play_InputtingSingleTick_Audio(key);

            switch (key.getType()) {
                case Math_Plus:
                case Math_Minus:
                case Math_Multiply:
                case Math_Divide:
                case Math_Equal:
                case Math_Dot:
                case Math_Percent: {
                    do_MathKey_Inputting(key);
                    break;
                }
            }
        }
    }

    @Override
    public InputList getInputList() {
        InputList topInputList = super.getInputList();

        Input<?> pending = topInputList.getPending();
        if (pending == null || !pending.isMathExpr()) {
            topInputList.withPending(new CharMathExprInput());
        }

        CharMathExprInput input = (CharMathExprInput) topInputList.getPending();
        return input.getInputList();
    }

    @Override
    protected void switchTo_Previous_Keyboard() {
        // 确保在切换到其他键盘前，当前表达式输入均完成提交
        getInputList().confirmPending();

        super.switchTo_Previous_Keyboard();
    }

    private void do_MathKey_Inputting(Key<?> key) {
        if (getInputList().hasEmptyPending()) {
            getInputList().newPending();
        }

        CharInput pending = getInputList().getPending();
        pending.appendKey(key);

        if (key instanceof CtrlKey) {
            switch (((CtrlKey) key).getType()) {
                case Math_Dot:
                case Math_Percent:
                    break;
                default:
                    getInputList().confirmPending();
            }
        }

        fire_InputChars_Inputting(getKeyFactory(), key);
    }
}
