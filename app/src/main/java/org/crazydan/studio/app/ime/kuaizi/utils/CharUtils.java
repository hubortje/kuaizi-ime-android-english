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

package org.crazydan.studio.app.ime.kuaizi.utils;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Paint;
import android.os.Build;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-25
 */
public class CharUtils {

    /** 部分中文占用的字节数不同，需要单独处理 */
    public static String[] getChars(String s) {
        List<String> chars = new ArrayList<>();

        // https://stackoverflow.com/questions/26357938/detect-chinese-character-in-java#answer-26358371
        // https://stackoverflow.com/questions/28761385/single-chinese-character-determined-as-length-2-in-java-scala-string#answer-28771484
        for (int i = 0; i < s.length(); ) {
            int codepoint = s.codePointAt(i);
            int charCount = Character.charCount(codepoint);

            String ch = s.substring(i, i + charCount);
            chars.add(ch);

            i += charCount;
        }
        return chars.toArray(new String[0]);
    }

    public static String fromUnicode(String unicode) {
        StringBuilder sb = new StringBuilder();

        // https://medium.com/swlh/how-to-easily-handle-emoji-unicode-in-java-ff905f264f98
        String[] codes = unicode.replace("U+", "0x").replace("\\u", "0x").split("\\s+");
        for (String code : codes) {
            int intCode = Integer.decode(code.trim());
            for (Character ch : Character.toChars(intCode)) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static boolean isPrintable(String s) {
        boolean hasGlyph = true;

        // https://stackoverflow.com/questions/11815458/check-if-custom-font-can-display-character#answer-47711610
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Paint paint = new Paint();
            hasGlyph = paint.hasGlyph(s);
        }
        return hasGlyph;
    }
}
