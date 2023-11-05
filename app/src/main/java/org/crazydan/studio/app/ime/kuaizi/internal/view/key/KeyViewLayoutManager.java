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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewLayoutManager;
import org.hexworks.mixite.core.api.Hexagon;
import org.hexworks.mixite.core.api.HexagonOrientation;
import org.hexworks.mixite.core.api.HexagonalGridBuilder;
import org.hexworks.mixite.core.api.HexagonalGridLayout;
import org.hexworks.mixite.core.api.Point;
import org.hexworks.mixite.core.api.contract.SatelliteData;

/**
 * {@link Keyboard 键盘}{@link Key 按键}的{@link RecyclerView}布局器
 * <p/>
 * 注意：<ul>
 * <li>通过按键布局视图的 margin 设置按键与父视图的间隙，
 * 从而降低通过 padding 设置间隙所造成的计算复杂度；</li>
 * </ul>
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class KeyViewLayoutManager extends RecyclerViewLayoutManager {
    private static final float cos_30 = (float) Math.cos(Math.toRadians(30));

    /** 按键正六边形方向 */
    private final HexagonOrientation gridItemOrientation;

    /** 按键正六边形半径：可见范围内的半径 */
    private float gridItemRadius;
    /** 按键正六边形内部边界半径：与可见边相交的圆 */
    private float gridItemInnerRadius;
    /** 按键正六边形外部边界半径：含按键间隔，并与顶点相交的圆 */
    private float gridItemOuterRadius;
    /** 按键间隔 */
    private float gridItemSpacing;
    /** 按键列数 */
    private int gridColumns;
    /** 按键行数 */
    private int gridRows;
    /** 网格左部空白 */
    private float gridPaddingLeft;
    /** 网格右部最大空白：配置数据，用于确保按键最靠近右手 */
    private float gridMaxPaddingRight;
    /** 网格顶部空白 */
    private float gridPaddingTop;
    /** 网格底部空白：实际剩余空间 */
    private float gridPaddingBottom;

    private boolean reverse;
    private boolean xPadEnabled;
    private List<RectHexagons> rectHexagonsList;

    public KeyViewLayoutManager(HexagonOrientation gridItemOrientation) {
        this.gridItemOrientation = gridItemOrientation;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public void enableXPad(boolean enabled) {
        this.xPadEnabled = enabled;
    }

    public void configGrid(int columns, int rows, int itemSpacingInDp, int gridMaxPaddingRight) {
        this.gridColumns = columns;
        this.gridRows = rows;
        this.gridItemSpacing = ScreenUtils.dpToPx(itemSpacingInDp);
        this.gridMaxPaddingRight = gridMaxPaddingRight;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);

        int itemCount = state.getItemCount();
        if (itemCount <= 0 || this.gridColumns <= 0 || this.gridRows <= 0) {
            return;
        }

        if (this.xPadEnabled) {
            layoutChildrenWithXPad(recycler, itemCount);
        } else {
            layoutChildren(recycler, itemCount);
        }
    }

    private List<RectHexagons> createGrid(HexagonOrientation orientation) {
        // Note: 只有布局完成后，才能得到视图的宽高
        // 按键按照行列数自动适配屏幕尺寸（以 POINTY_TOP 方向为例，FLAT_TOP 方向为 POINTY_TOP 方向的 xy 轴交换）
        // - 这里按照按键紧挨的情况计算正六边形的半径，
        //   再在绘制时按照按键间隔计算正六边形的实际绘制半径
        // - 相邻六边形中心间距的计算公式见: https://www.redblobgames.com/grids/hexagons/#spacing
        // - 横向半径（r1）与宽度（w）的关系: w = (2 * n + 1) * r1 * cos30
        // - 纵向半径（r2）与高度（h）的关系: h = 2 * r2 + (m - 1) * cos30 * (2 * r2 * cos30)
        // - 最终正六边形的半径: radius = Math.min(r1, r2)
        // - 按键实际绘制半径（减去一半间距）: R = radius - spacing / (2 * cos30)
        int w = orientation == HexagonOrientation.POINTY_TOP ? getWidth() : getHeight();
        int h = orientation == HexagonOrientation.POINTY_TOP ? getHeight() : getWidth();
        int n = orientation == HexagonOrientation.POINTY_TOP ? this.gridColumns : this.gridRows;
        int m = orientation == HexagonOrientation.POINTY_TOP ? this.gridRows : this.gridColumns;
        float r1 = w / ((2 * n + 1) * cos_30);
        float r2 = h / (2 * ((m - 1) * cos_30 * cos_30 + 1));

        // 计算左上角的空白偏移量
        this.gridPaddingLeft = this.gridPaddingTop = 0;
        this.gridPaddingBottom = 0;

        float radius;
        int compare = Double.compare(r1, r2);
        if (compare < 0) {
            radius = r1;

            float h_used = 2 * radius + (m - 1) * cos_30 * (2 * radius * cos_30);
            float h_left = h - h_used;
            float paddingY = h_left / 2;
            if (orientation == HexagonOrientation.POINTY_TOP) {
                this.gridPaddingTop = paddingY;
                this.gridPaddingBottom = paddingY;
            } else {
                this.gridPaddingLeft = Math.max(0, h_left - this.gridMaxPaddingRight);
            }
        } else if (compare > 0) {
            radius = r2;

            float w_used = (2 * n + 1) * radius * cos_30;
            float w_left = w - w_used;
            float paddingX = w_left / 2;
            if (orientation == HexagonOrientation.POINTY_TOP) {
                this.gridPaddingLeft = Math.max(0, w_left - this.gridMaxPaddingRight);
            } else {
                this.gridPaddingTop = paddingX;
                this.gridPaddingBottom = paddingX;
            }
        } else {
            radius = r1;
        }

        this.gridItemRadius = radius - this.gridItemSpacing / (2 * cos_30);
        this.gridItemOuterRadius = radius;
        this.gridItemInnerRadius = this.gridItemRadius * cos_30;

        float hexagonRectWidth = this.gridItemInnerRadius * 2 + this.gridItemSpacing;
        float rectTop = this.gridPaddingTop;
        float rectLeft = Math.min(this.gridPaddingLeft, this.gridMaxPaddingRight);
        float rectHeight = getHeight() - this.gridPaddingBottom;
        float leftRectRight = rectLeft + hexagonRectWidth;
        float rightRectRight = getWidth() - rectLeft;
        float rightRectLeft = rightRectRight //
                              - (orientation == HexagonOrientation.POINTY_TOP
                                 ? hexagonRectWidth * 1.5f
                                 : hexagonRectWidth);
        int middleGridColumns = this.gridColumns - 2;
        float middleRectLeft = this.xPadEnabled //
                               ? leftRectRight //
                               // 靠右对齐
                               : rightRectLeft - hexagonRectWidth * middleGridColumns;

        // 将键盘拆分为左、中、右三个部分，左部分的往最左侧靠齐，中和右部分往最右侧靠齐
        List<RectHexagons> rectHexagonsList = new ArrayList<>();
        RectHexagons leftRectHexagons = new RectHexagons(this.gridItemRadius, (idx) -> idx * this.gridColumns);
        leftRectHexagons.rect.set(rectLeft, rectTop, leftRectRight, rectHeight);
        leftRectHexagons.generate(1, this.gridRows, orientation, radius);
        rectHexagonsList.add(leftRectHexagons);

        RectHexagons rightRectHexagons = new RectHexagons(this.gridItemRadius,
                                                          (idx) -> (idx + 1) * this.gridColumns - 1);
        rightRectHexagons.rect.set(rightRectLeft, rectTop, rightRectRight, rectHeight);
        rightRectHexagons.generate(1, this.gridRows, orientation, radius);
        rectHexagonsList.add(rightRectHexagons);

        RectHexagons middleRectHexagons = new RectHexagons(this.gridItemRadius,
                                                           (idx) -> idx + (2 * (idx / middleGridColumns) + 1));
        middleRectHexagons.rect.set(middleRectLeft, rectTop, rightRectHexagons.rect.left, rectHeight);
        middleRectHexagons.generate(middleGridColumns, this.gridRows, orientation, radius);
        rectHexagonsList.add(1, middleRectHexagons);

        return rectHexagonsList;
    }

    public double getGridPaddingBottom() {
        return this.gridPaddingBottom;
    }

    /**
     * 找出指定坐标下的子视图
     * <p/>
     * 探测范围比 {@link #findChildViewUnder} 更大，
     * 但比 {@link #findChildViewNear} 更小
     */
    public View findChildViewUnderLoose(double x, double y) {
        return filterChildViewByHexagonCenterDistance(x, y, distance -> distance < this.gridItemRadius);
    }

    /**
     * 找出指定坐标下的子视图
     * <p/>
     * 仅当坐标在正六边形的内圈中时才视为符合条件
     */
    public View findChildViewUnder(double x, double y) {
        return filterChildViewByHexagonCenterDistance(x, y, distance -> distance < this.gridItemInnerRadius);
    }

    /**
     * 找出靠近指定坐标的子视图
     * <p/>
     * 仅当坐标在正六边形的外圈但不在内圈中时才视为符合条件
     */
    public View findChildViewNear(double x, double y, int deltaInDp) {
        int delta = ScreenUtils.dpToPx(deltaInDp);
        double outer = this.gridItemOuterRadius + delta;

        return filterChildViewByHexagonCenterDistance(x,
                                                      y,
                                                      distance -> distance > this.gridItemInnerRadius
                                                                  && distance < outer);
    }

    private View filterChildViewByHexagonCenterDistance(double x, double y, Predicate<Double> predicate) {
        Point point = Point.fromPosition(x, y);

//        int i = 0;
//        int itemCount = getItemCount();
//        for (Hexagon<SatelliteData> hexagon : this.grid.getHexagons()) {
//            if (i >= itemCount) {
//                break;
//            }
//
//            Point center = coord(hexagon.getCenter());
//            double distance = center.distanceFrom(point);
//
//            if (predicate.test(distance)) {
//                return getChildAt(i);
//            }
//            i++;
//        }

        return null;
    }

    private Point coord(Point point) {
        return coord(point.getCoordinateX(), point.getCoordinateY());
    }

    private Point coord(double x, double y) {
        x += this.xPadEnabled ? Math.min(this.gridItemSpacing, this.gridPaddingLeft) : this.gridPaddingLeft;
        y += this.gridPaddingTop;

        if (this.reverse) {
            return Point.fromPosition(getWidth() - x, y);
        }
        return Point.fromPosition(x, y);
    }

    private void layoutChildren(RecyclerView.Recycler recycler, int itemCount) {
        this.rectHexagonsList = createGrid(this.gridItemOrientation);

        for (RectHexagons rectHexagons : this.rectHexagonsList) {
            for (IndexedHexagon hexagon : rectHexagons.hexagons) {
                if (hexagon.index >= itemCount) {
                    break;
                }

                View view = recycler.getViewForPosition(hexagon.index);
                addView(view);

                layoutHexagonItemView(view, hexagon.hexagon, rectHexagons);
            }
        }
    }

    private void layoutChildrenWithXPad(RecyclerView.Recycler recycler, int itemCount) {
        this.rectHexagonsList = createGrid(HexagonOrientation.FLAT_TOP);

        View xPadKeyView = null;
        RectF xPadKeyViewRect = null;
        int xPadKeyViewType = KeyViewAdapter.getKeyViewType(new XPadKey());

        for (RectHexagons rectHexagons : this.rectHexagonsList) {
            for (IndexedHexagon hexagon : rectHexagons.hexagons) {
                if (hexagon.index >= itemCount) {
                    break;
                }

                View view = recycler.getViewForPosition(hexagon.index);
                addView(view);

                if (getItemViewType(view) == xPadKeyViewType) {
                    xPadKeyView = view;
                    xPadKeyViewRect = rectHexagons.rect;
                    continue;
                }

                layoutHexagonItemView(view, hexagon.hexagon, rectHexagons);
            }
        }

        if (xPadKeyView != null) {
            int left = (int) xPadKeyViewRect.left;
            int right = (int) xPadKeyViewRect.right;

            layoutItemView(xPadKeyView,
                           Math.min(left, right),
                           (int) xPadKeyViewRect.top,
                           Math.max(left, right),
                           (int) xPadKeyViewRect.bottom);
        }
    }

    private void layoutItemView(View view, int left, int top, int right, int bottom) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = Math.abs(left - right);
        layoutParams.height = Math.abs(top - bottom);

        measureChildWithMargins(view, 0, 0);
        layoutDecoratedWithMargins(view, left, top, right, bottom);
    }

    private void layoutHexagonItemView(
            View view, Hexagon<SatelliteData> hexagon, RectHexagons rectHexagons
    ) {
        // 按按键半径调整按键视图的宽高
        Point center = hexagon.getCenter();
        RectF rect = rectHexagons.rect;
        float radius = rectHexagons.radius;
        float x = (float) (center.getCoordinateX() + rect.left);
        float y = (float) (center.getCoordinateY() + rect.top);

        int left = Math.round(x - radius);
        int top = Math.round(y - radius);
        int right = Math.round(x + radius);
        int bottom = Math.round(y + radius);

        float minSize = ScreenUtils.dpToPx(52);
        int actualSize = Math.round(radius * 2);
        if (actualSize < minSize) {
            float scale = actualSize / minSize;

            for (int j = 0; j < ((ViewGroup) view).getChildCount(); j++) {
                View child = ((ViewGroup) view).getChildAt(j);
                if (child.getId() == R.id.bg_view) {
                    continue;
                }

                child.setScaleX(scale);
                child.setScaleY(scale);
            }
        }

        layoutItemView(view, left, top, right, bottom);
    }

    private static class RectHexagons {
        public final float radius;
        public final RectF rect = new RectF();
        public final List<IndexedHexagon> hexagons = new ArrayList<>();

        private final Function<Integer, Integer> indexer;

        private RectHexagons(float radius, Function<Integer, Integer> indexer) {
            this.radius = radius;
            this.indexer = indexer;
        }

        public void generate(int columns, int rows, HexagonOrientation orientation, float radius) {
            HexagonalGridBuilder<SatelliteData> builder = new HexagonalGridBuilder<>();
            builder.setGridWidth(columns)
                   .setGridHeight(rows)
                   .setGridLayout(HexagonalGridLayout.RECTANGULAR)
                   .setOrientation(orientation)
                   // 包含间隔的半径
                   .setRadius(radius);

            int i = 0;
            Iterable<Hexagon<SatelliteData>> it = builder.build().getHexagons();
            for (Hexagon<SatelliteData> hexagon : it) {
                int index = this.indexer.apply(i++);
                this.hexagons.add(new IndexedHexagon(index, hexagon));
            }
        }
    }

    private static class IndexedHexagon {
        public final int index;
        public final Hexagon<SatelliteData> hexagon;

        private IndexedHexagon(int index, Hexagon<SatelliteData> hexagon) {
            this.index = index;
            this.hexagon = hexagon;
        }
    }
}
