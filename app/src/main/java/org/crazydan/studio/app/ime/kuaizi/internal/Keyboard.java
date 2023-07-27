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

package org.crazydan.studio.app.ime.kuaizi.internal;

import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;

/**
 * 键盘
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public interface Keyboard {

    KeyFactory getKeyFactory();

    void setInputList(InputList inputList);

    /** 重置状态 */
    void reset();

    /** 处理{@link UserKeyMsg 按键消息} */
    void onUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data);

    /**
     * 添加{@link InputMsg 输入消息监听}
     * <p/>
     * 忽略重复加入的监听，且执行顺序与添加顺序无关
     */
    void addInputMsgListener(InputMsgListener listener);

    /** 键盘类型 */
    enum Type {
        /** 汉语拼音键盘 */
        Pinyin,
        /** 拉丁文（含字母和数字）键盘：支持数字计算 */
        Latin,
        /** 数字键盘：纯数字 */
        Number,
        /** 电话号码键盘：纯数字加 #、* 等 */
        Phone,
        /** 标点符号键盘：可中英文符号切换 */
        Symbol,
        /** 表情符号键盘 */
        Emotion,
    }

    /** 键盘布局方向 */
    enum Orientation {
        /** 纵向 */
        Portrait,
        /** 横向 */
        Landscape,
    }

    /** 按键生成器 */
    interface KeyFactory {
        /**
         * 创建二维矩阵{@link Key 按键}
         * <p/>
         * 元素可为<code>null</code>，
         * 表示该位置不放置任何按键
         */
        Key<?>[][] create(Option option);

        class Option {
            public Orientation orientation;
        }
    }
}
