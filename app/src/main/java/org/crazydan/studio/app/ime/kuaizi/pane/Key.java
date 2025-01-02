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

package org.crazydan.studio.app.ime.kuaizi.pane;

import java.util.Objects;

import android.util.LruCache;
import org.crazydan.studio.app.ime.kuaizi.common.ImmutableBuilder;

/**
 * {@link Keyboard} 上的按键
 * <p/>
 * 注意，其为只读模型，不可对其进行变更，从而确保其实例可被缓存
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public abstract class Key {
    /** 按键的输入值，代表字符按键的实际输入字符，其与{@link #label 显示字符}可能并不相等 */
    public final String value;
    /** 按键上显示的文字内容 */
    public final String label;

    /** 按键上显示的图标资源 id */
    public final Integer icon;
    /** 按键的配色，始终不为 null */
    public final Color color;

    /** 按键是否已被禁用 */
    public final boolean disabled;

    /**
     * 当前对象实例的 Hash 值，其将作为 {@link #hashCode()} 的返回值，
     * 并且用于判断对象是否{@link #equals(Object) 相等}
     * <p/>
     * 该值与其{@link Builder 构造器}的 {@link Builder#hashCode()} 相等，
     * 因为二者的属性值是全部相等的
     */
    private final int objHash;

    protected Key(Builder<?, ?> builder) {
        this.value = builder.value;
        this.label = builder.label;

        this.icon = builder.icon;
        this.color = builder.color;

        this.disabled = builder.disabled;

        this.objHash = builder.hashCode();
    }

    @Override
    public String toString() {
        return this.label + "(" + this.value + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Key that = (Key) o;
        return this.objHash == that.objHash;
    }

    @Override
    public int hashCode() {
        return this.objHash;
    }

    /** {@link Key} 配色 */
    public static class Color {
        /** 前景色资源 id */
        public final Integer fg;
        /** 背景色资源 id */
        public final Integer bg;

        private Color(Integer fg, Integer bg) {
            this.fg = fg;
            this.bg = bg;
        }

        public static Color create(Integer fg, Integer bg) {
            return new Color(fg, bg);
        }

        public static Color none() {
            return create(null, null);
        }
    }

    /**
     * {@link Key} 构建器，用于以链式调用形式配置按键属性，并支持缓存按键，从而避免反复创建相同的 {@link Key}
     * <p/>
     * 注意，构建器本身需采用单例模式，并且在 {@link #build} 时，以其 {@link #hashCode()} 作为按键缓存的唯一索引，
     * 因此，构建器不是线程安全的
     */
    protected static abstract class Builder< //
            B extends Builder<B, K>, //
            K extends Key //
            > extends ImmutableBuilder<B, K> {
        final LruCache<Integer, K> cache;

        private String value;
        private String label;

        private Integer icon;
        private Color color = Color.none();

        private boolean disabled;

        /**
         * @param cacheSize
         *         可缓存的 {@link  Key} 数量
         */
        protected Builder(int cacheSize) {
            this.cache = cacheSize > 0 ? new LruCache<>(cacheSize) : null;
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected void reset() {
            this.value = null;
            this.label = null;

            this.icon = null;
            this.color = Color.none();

            this.disabled = false;
        }

        @Override
        protected K build() {
            int hash = hashCode();

            K key = this.cache != null ? this.cache.get(hash) : null;
            if (key == null) {
                key = doBuild();

                if (this.cache != null) {
                    this.cache.put(hash, key);
                }
            }

            return key;
        }

        /**
         * 在 {@link Key} 的构造函数中根据 {@link Builder} 为只读属性赋初始值
         * <p/>
         * 注意，相关属性的值转换和处理操作需在传给 {@link Key} 的构造函数之前完成，
         * 以使其构造函数内仅需直接引用构建器的属性值，确保二者的 {@link #hashCode()} 是相同的
         */
        protected abstract K doBuild();

        /** 通过构建器的 hash 值作为按键缓存的索引 */
        @Override
        public int hashCode() {
            return Objects.hash(this.value, this.label, this.disabled, this.icon, this.color.fg, this.color.bg);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 按键配置 ===================

        /**
         * 创建与指定 {@link Key} 相同的按键，并可继续按需修改其他配置
         * <p/>
         * 注意，{@link Key#disabled} 状态不做复制，需按需单独处理
         */
        public B from(K key) {
            return value(key.value).label(key.label).icon(key.icon).color(key.color);
        }

        /** @see Key#value */
        public B value(String value) {
            this.value = value;
            return (B) this;
        }

        public String value() {return this.value;}

        /** @see Key#label */
        public B label(String label) {
            this.label = label;
            return (B) this;
        }

        public String label() {return this.label;}

        /** @see Key#icon */
        public B icon(Integer icon) {
            this.icon = icon;
            return (B) this;
        }

        /**
         * @param color
         *         若为 null，则赋值为 {@link Color#none()}
         * @see Key#color
         */
        public B color(Color color) {
            this.color = color != null ? color : Color.none();
            return (B) this;
        }

        /** 设置按键是否被禁用 */
        public B disabled(boolean disabled) {
            this.disabled = disabled;
            return (B) this;
        }

        /** 禁用按键 */
        public B disable() {
            return disabled(true);
        }

        // ===================== End: 按键配置 ===================
    }
}
