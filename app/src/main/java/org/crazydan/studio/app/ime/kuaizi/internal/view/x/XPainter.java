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

package org.crazydan.studio.app.ime.kuaizi.internal.view.x;

import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import org.crazydan.studio.app.ime.kuaizi.utils.ThemeUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-01
 */
public class XPainter {
    public final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    public XPainter() {
        this.fill.setStyle(Paint.Style.FILL);
        this.fill.setColor(Color.TRANSPARENT);

        this.stroke.setStyle(Paint.Style.STROKE);
        this.stroke.setColor(Color.TRANSPARENT);
    }

    public void setFillColor(int color) {
        this.fill.setColor(color);
    }

    public void setStrokeStyle(String style) {
        ThemeUtils.applyBorder(this.stroke, style);
    }

    public void setCornerRadius(float radius) {
        CornerPathEffect effect = new CornerPathEffect(radius);

        this.fill.setPathEffect(effect);
        this.stroke.setPathEffect(effect);
    }
}
