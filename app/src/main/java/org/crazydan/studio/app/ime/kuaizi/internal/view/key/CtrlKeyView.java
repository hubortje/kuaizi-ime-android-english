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

package org.crazydan.studio.app.ime.kuaizi.internal.view.key;

import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link Keyboard 键盘}{@link CtrlKey 控制按键}的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-02
 */
public class CtrlKeyView extends KeyView<CtrlKey, ImageView> {

    public CtrlKeyView(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(CtrlKey key, HexagonOrientation orientation) {
        super.bind(key, orientation);

        if (key.getIconResId() > 0) {
            this.fgView.setImageResource(key.getIconResId());
        } else {
            // 存在复用的情况，故，需对其他情况对前景图片进行置空
            this.fgView.setImageDrawable(null);
        }
    }
}
