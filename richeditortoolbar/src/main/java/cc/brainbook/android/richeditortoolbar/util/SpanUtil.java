package cc.brainbook.android.richeditortoolbar.util;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import cc.brainbook.android.richeditortoolbar.bean.SpanBean;
import cc.brainbook.android.richeditortoolbar.bean.TextBean;
import cc.brainbook.android.richeditortoolbar.span.CustomImageSpan;

public abstract class SpanUtil {

    public static <T> ArrayList<T> getFilteredSpans(final Editable editable, Class<T> clazz, int start, int end) {
        final ArrayList<T> filteredSpans = new ArrayList<>();
        final T[] spans = editable.getSpans(start, end, clazz);

        ///在Android6.0 以下这个方法返回的数组是有顺序的，但是7.0以上系统这个方法返回的数组顺序有错乱，所以我们需要自己排序
        ///https://stackoverflow.com/questions/41052172/spannablestringbuilder-getspans-sort-order-is-wrong-on-nougat-7-0-7-1
        ///https://www.jianshu.com/p/57783747e530
        ///按照span起始位置从小到大排序
        Arrays.sort(spans, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return editable.getSpanStart(o1) - editable.getSpanStart(o2);
            }
        });

        for (T span : spans) {
            ///忽略不是clazz本身（比如为clazz的子类）的span
            ///getSpans()获取clazz类及其子类
            ///比如：HeadSpan extends AbsoluteSizeSpan：
            ///editable.getSpans(start, end, AbsoluteSizeSpan)也能获取到AbsoluteSizeSpan的子类HeadSpan
            if (span.getClass() != clazz) {
//                editable.removeSpan(span);///注意：千万不要remove！因为有可能是子类！
                continue;
            }

            final int spanStart = editable.getSpanStart(span);
            final int spanEnd = editable.getSpanEnd(span);
            ///删除多余的span
            if (spanStart == spanEnd) {
                editable.removeSpan(span);
                continue;
            }

            filteredSpans.add(span);
        }

        return filteredSpans;
    }

    public static <T> ArrayList<T> getSelectedSpans(EditText editText, Class<T> clazz) {
        ArrayList<T> filteredSpans = new ArrayList<>();
        final int selectionStart = editText.getSelectionStart();
        final int selectionEnd = editText.getSelectionEnd();
        if (selectionStart != -1 && selectionEnd != -1) { ///-1 if there is no selection or cursor
            filteredSpans = SpanUtil.getFilteredSpans(editText.getText(), clazz, selectionStart, selectionEnd);
        }
        return filteredSpans;
//        return (T[]) Array.newInstance(clazz);  ///https://bbs.csdn.net/topics/370137571, https://blog.csdn.net/qing0706/article/details/51067981
    }

    public static int getParagraphStart(Editable editable, int where) {
        ///DynamicLayout#reflow(CharSequence s, int where, int before, int after)
        // seek back to the start of the paragraph
        int find = TextUtils.lastIndexOf(editable, '\n', where - 1);
        if (find < 0)
            find = 0;
        else
            find++;

        return find;
    }

    public static int getParagraphEnd(Editable editable, int where) {
        ///DynamicLayout#reflow(CharSequence s, int where, int before, int after)
        // seek forward to the end of the paragraph
        int len = editable.length();
        int look = TextUtils.indexOf(editable, '\n', where);
        if (look < 0)
            look = len;
        else
            look++; // we want the index after the \n

        return look;
    }

    /**
     * 清除区间内的span
     */
    public static <T> void removeSpans(Class<T> clazz, Editable editable, int start, int end) {
        final ArrayList<T> spans = SpanUtil.getFilteredSpans(editable, clazz, start, end);
        for (T span : spans) {
            final int spanStart = editable.getSpanStart(span);
            final int spanEnd = editable.getSpanEnd(span);
            if (start <= spanStart && spanEnd <= end) {
                editable.removeSpan(span);
            }
        }
    }

    /**
     * 平摊并合并交叉重叠的同类span
     *
     * 本编辑器内部添加逻辑不会产生交叉重叠，以防从编辑器外部或HTML转换后可能会产生的交叉重叠
     * 注意：暂时没有考虑ForegroundColor、BackgroundColor等带参数的span！即参数不同的同类span都视为相等而合并
     */
    public static <T> void flatSpans(Class<T> clazz, Editable editable, int start, int end) {
        T currentSpan = null;
        final ArrayList<T> spans = SpanUtil.getFilteredSpans(editable, clazz, start, end);
        for (T span : spans) {
            final int spanStart = editable.getSpanStart(span);
            final int spanEnd = editable.getSpanEnd(span);

            if (currentSpan == null) {
                currentSpan = span;
                continue;
            }
            if (currentSpan == span) {
                continue;
            }
            final int currentSpanStart = editable.getSpanStart(currentSpan);
            final int currentSpanEnd = editable.getSpanEnd(currentSpan);
            if (currentSpanEnd < spanStart) {
                currentSpan = span;
                continue;
            }
            if (currentSpanStart >= spanStart && currentSpanEnd <= spanEnd) {
                editable.removeSpan(currentSpan);
                currentSpan = span;
                continue;
            }
            if (currentSpanStart <= spanStart && currentSpanEnd <= spanEnd) {
                editable.setSpan(currentSpan, currentSpanStart, spanEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            } else if (currentSpanStart >= spanStart) {
                editable.setSpan(currentSpan, spanStart, currentSpanEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
            editable.removeSpan(span);
        }
    }

    public static CustomImageSpan getImageSpan(Editable editable, Drawable drawable) {
        CustomImageSpan imageSpan = null;
        if (!TextUtils.isEmpty(editable)) {
            final CustomImageSpan[] spans = editable.getSpans(0, editable.length(), CustomImageSpan.class);
            if (spans != null && spans.length > 0) {
                for (CustomImageSpan span : spans) {
                    if (drawable == span.getDrawable()) {
                        imageSpan = span;
                    }
                }
            }
        }

        return imageSpan;
    }

    public static <T extends Parcelable> void addSpanBeans(List<SpanBean> spanBeans, Class<T> clazz, Editable editable, int start, int end) {
        final ArrayList<T> spans = SpanUtil.getFilteredSpans(editable, clazz, start, end);
        for (T span : spans) {
            ///注意：必须过滤掉没有CREATOR变量的span！
            ///理论上，所有RichEditor用到的span都应该自定义、且直接实现Parcelable（即该span类直接包含CREATOR变量），否则予以忽略
            try {
                clazz.getField("CREATOR");
                final int spanStart = editable.getSpanStart(span);
                final int spanEnd = editable.getSpanEnd(span);
                final int spanFlags = editable.getSpanFlags(span);
                final int adjustSpanStart = spanStart < start ? 0 : spanStart - start;
                final int adjustSpanEnd = (spanEnd > end ? end : spanEnd) - start;
                final SpanBean<T> spanBean = new SpanBean<>(span, adjustSpanStart, adjustSpanEnd, spanFlags);
                spanBeans.add(spanBean);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadSpanBeans(List<SpanBean> spanBeans, Editable editable) {
        for (SpanBean spanBean : spanBeans) {
            final int spanStart = spanBean.getSpanStart();
            final int spanEnd = spanBean.getSpanEnd();
            final int spanFlags = spanBean.getSpanFlags();
            final Parcelable span = spanBean.getSpan();
            editable.setSpan(span, spanStart, spanEnd, spanFlags);
        }
    }


    public static void saveSpansSelection(HashMap<View, Class> classHashMap, File draftFile, Editable editable, int selectionStart, int selectionEnd) {
        final TextBean textBean = new TextBean();
        final CharSequence subSequence = editable.subSequence(selectionStart, selectionEnd);
        textBean.setText(subSequence.toString());

        final ArrayList<SpanBean> spanBeans = new ArrayList<>();
        for (Class clazz : classHashMap.values()) {
            SpanUtil.addSpanBeans(spanBeans, clazz, editable, selectionStart, selectionEnd);
        }
        textBean.setSpans(spanBeans);

        final byte[] bytes = ParcelableUtil.marshall(textBean);

        FileUtil.writeFile(draftFile, Base64.encodeToString(bytes, 0));
    }

    ///从进程App共享空间恢复spans
    public static void loadSpans(File draftFile, Editable editable) {
        final String draftText = FileUtil.readFile(draftFile);
        if (TextUtils.isEmpty(draftText)) {
            return;
        }

        final Parcel parcel = ParcelableUtil.unmarshall(Base64.decode(draftText, Base64.DEFAULT));
        final TextBean textBean = TextBean.CREATOR.createFromParcel(parcel);
        //////??????[BUG#ClipDescription的label总是为“host clipboard”]因此无法用label区分剪切板是否为RichEditor或其它App，只能用文本是否相同来“大约”区分
        if (!TextUtils.equals(textBean.getText(), editable)) {
            return;
        }

        ///注意：清除原有的span，比如BoldSpan的父类StyleSpan
        editable.clearSpans();

        final List<SpanBean> spanBeans = textBean.getSpans();
        SpanUtil.loadSpanBeans(spanBeans, editable);
    }

    ///test
    public static <T> void testOutput(Editable editable, Class<T> clazz) {
        final T[] spans = editable.getSpans(0, editable.length(), clazz);
        for (T span : spans) {
            ///忽略getSpans()获取的子类（不是clazz本身）
            if (span.getClass() != clazz) {
                continue;
            }

            final int spanStart = editable.getSpanStart(span);
            final int spanEnd = editable.getSpanEnd(span);
            Log.d("TAG", span.getClass().getSimpleName() + ": " + spanStart + ", " + spanEnd);
        }
    }

}
