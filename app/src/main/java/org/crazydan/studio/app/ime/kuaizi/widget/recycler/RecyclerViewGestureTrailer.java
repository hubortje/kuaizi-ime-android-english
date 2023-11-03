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

package org.crazydan.studio.app.ime.kuaizi.widget.recycler;

import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.widget.ViewGestureTrailer;

/**
 * 在 {@link RecyclerView} 之上绘制滑屏轨迹
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-27
 */
public class RecyclerViewGestureTrailer extends ViewGestureTrailer implements ViewGestureDetector.Listener {
    private final RecyclerView recyclerView;

    public RecyclerViewGestureTrailer(RecyclerView recyclerView, boolean disabled) {
        this.recyclerView = recyclerView;

        setDisabled(disabled);
    }

    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        if (!isDisabled()) {
            this.recyclerView.invalidate();
        }

        super.onGesture(type, data);
    }
}
