package cc.brainbook.android.richeditortoolbar.helper;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.text.TextDirectionHeuristicsCompat;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.BulletSpan;
import android.text.style.CharacterStyle;
import android.text.style.ParagraphStyle;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;

import org.ccil.cowan.tagsoup.HTMLSchema;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.brainbook.android.richeditortoolbar.span.AlignCenterSpan;
import cc.brainbook.android.richeditortoolbar.span.AlignNormalSpan;
import cc.brainbook.android.richeditortoolbar.span.AlignOppositeSpan;
import cc.brainbook.android.richeditortoolbar.span.AudioSpan;
import cc.brainbook.android.richeditortoolbar.span.BoldSpan;
import cc.brainbook.android.richeditortoolbar.span.CustomAbsoluteSizeSpan;
import cc.brainbook.android.richeditortoolbar.span.CustomBackgroundColorSpan;
import cc.brainbook.android.richeditortoolbar.span.CustomFontFamilySpan;
import cc.brainbook.android.richeditortoolbar.span.CustomForegroundColorSpan;
import cc.brainbook.android.richeditortoolbar.span.CustomImageSpan;
import cc.brainbook.android.richeditortoolbar.span.CustomLeadingMarginSpan;
import cc.brainbook.android.richeditortoolbar.span.CustomQuoteSpan;
import cc.brainbook.android.richeditortoolbar.span.CustomRelativeSizeSpan;
import cc.brainbook.android.richeditortoolbar.span.CustomStrikethroughSpan;
import cc.brainbook.android.richeditortoolbar.span.CustomSubscriptSpan;
import cc.brainbook.android.richeditortoolbar.span.CustomSuperscriptSpan;
import cc.brainbook.android.richeditortoolbar.span.CustomURLSpan;
import cc.brainbook.android.richeditortoolbar.span.CustomUnderlineSpan;
import cc.brainbook.android.richeditortoolbar.span.DivSpan;
import cc.brainbook.android.richeditortoolbar.span.HeadSpan;
import cc.brainbook.android.richeditortoolbar.span.ItalicSpan;
import cc.brainbook.android.richeditortoolbar.span.LineDividerSpan;
import cc.brainbook.android.richeditortoolbar.span.ListItemSpan;
import cc.brainbook.android.richeditortoolbar.span.ListSpan;
import cc.brainbook.android.richeditortoolbar.span.NestSpan;
import cc.brainbook.android.richeditortoolbar.span.VideoSpan;
import cc.brainbook.android.richeditortoolbar.util.SpanUtil;

import static cc.brainbook.android.richeditortoolbar.helper.ListSpanHelper.LIST_TYPE_ORDERED_DECIMAL;
import static cc.brainbook.android.richeditortoolbar.helper.ListSpanHelper.LIST_TYPE_ORDERED_LOWER_LATIN;
import static cc.brainbook.android.richeditortoolbar.helper.ListSpanHelper.LIST_TYPE_ORDERED_LOWER_ROMAN;
import static cc.brainbook.android.richeditortoolbar.helper.ListSpanHelper.LIST_TYPE_ORDERED_UPPER_LATIN;
import static cc.brainbook.android.richeditortoolbar.helper.ListSpanHelper.LIST_TYPE_ORDERED_UPPER_ROMAN;
import static cc.brainbook.android.richeditortoolbar.helper.ListSpanHelper.LIST_TYPE_UNORDERED_DISC;
import static cc.brainbook.android.richeditortoolbar.helper.ListSpanHelper.LIST_TYPE_UNORDERED_CIRCLE;
import static cc.brainbook.android.richeditortoolbar.helper.ListSpanHelper.LIST_TYPE_UNORDERED_SQUARE;
import static cc.brainbook.android.richeditortoolbar.helper.ListSpanHelper.isListTypeOrdered;
import static cc.brainbook.android.richeditortoolbar.helper.RichEditorToolbarHelper.getSpanFlags;

/**
 * This class processes HTML strings into displayable styled text.
 * Not all HTML tags are supported.
 */
public class Html {
    ///[UPGRADE#android.text.Html]缺省的屏幕密度
    ///px in CSS is the equivalance of dip in Android
    ///注意：一般情况下，CustomAbsoluteSizeSpan的dip都为true，否则需要在使用Html之前设置本机的具体准确的屏幕密度！
    public static float sDisplayMetricsDensity = 3.0f;

    /**
     * Retrieves images for HTML &lt;img&gt; tags.
     */
    public static interface ImageGetter {
        /**
         * This method is called when the HTML parser encounters an
         * &lt;img&gt; tag.  The <code>source</code> argument is the
         * string from the "src" attribute; the return value should be
         * a Drawable representation of the image or <code>null</code>
         * for a generic replacement image.  Make sure you call
         * setBounds() on your Drawable if it doesn't already have
         * its bounds set.
         */
        public Drawable getDrawable(String source);
    }

    /**
     * Is notified when HTML tags are encountered that the parser does
     * not know how to interpret.
     */
    public static interface TagHandler {
        /**
         * This method will be called whenn the HTML parser encounters
         * a tag that it does not know how to interpret.
         */
        public void handleTag(boolean opening, String tag,
                              Editable output, XMLReader xmlReader);
    }

    /**
     * Option for {@link #toHtml(Spanned, int)}: Wrap consecutive lines of text delimited by '\n'
     * inside &lt;p&gt; elements. {@link BulletSpan}s are ignored.
     */
    public static final int TO_HTML_PARAGRAPH_LINES_CONSECUTIVE = 0x00000000;

    /**
     * Option for {@link #toHtml(Spanned, int)}: Wrap each line of text delimited by '\n' inside a
     * &lt;p&gt; or a &lt;li&gt; element. This allows {@link ParagraphStyle}s attached to be
     * encoded as CSS styles within the corresponding &lt;p&gt; or &lt;li&gt; element.
     */
    public static final int TO_HTML_PARAGRAPH_LINES_INDIVIDUAL = 0x00000001;

    /**
     * Flag indicating that texts inside &lt;p&gt; elements will be separated from other texts with
     * one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH = 0x00000001;

    /**
     * Flag indicating that texts inside &lt;h1&gt;~&lt;h6&gt; elements will be separated from
     * other texts with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_HEADING = 0x00000002;

    /**
     * Flag indicating that texts inside &lt;li&gt; elements will be separated from other texts
     * with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM = 0x00000004;

    /**
     * Flag indicating that texts inside &lt;ul&gt; elements will be separated from other texts
     * with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_LIST = 0x00000008;

    /**
     * Flag indicating that texts inside &lt;div&gt; elements will be separated from other texts
     * with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_DIV = 0x00000010;

    /**
     * Flag indicating that texts inside &lt;blockquote&gt; elements will be separated from other
     * texts with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE = 0x00000020;

    /**
     * Flag indicating that CSS color values should be used instead of those defined in
     * {@link Color}.
     */
    public static final int FROM_HTML_OPTION_USE_CSS_COLORS = 0x00000100;

    /**
     * Flags for {@link #fromHtml(String, int, Html.ImageGetter, Html.TagHandler)}: Separate block-level
     * elements with blank lines (two newline characters) in between. This is the legacy behavior
     * prior to N.
     */
    public static final int FROM_HTML_MODE_LEGACY = 0x00000000;

    /**
     * Flags for {@link #fromHtml(String, int, Html.ImageGetter, Html.TagHandler)}: Separate block-level
     * elements with line breaks (single newline character) in between. This inverts the
     * {@link Spanned} to HTML string conversion done with the option
     * {@link #TO_HTML_PARAGRAPH_LINES_INDIVIDUAL}.
     */
    public static final int FROM_HTML_MODE_COMPACT =
            FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH
                    | FROM_HTML_SEPARATOR_LINE_BREAK_HEADING
                    | FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM
                    | FROM_HTML_SEPARATOR_LINE_BREAK_LIST
                    | FROM_HTML_SEPARATOR_LINE_BREAK_DIV
                    | FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE;

    /**
     * The bit which indicates if lines delimited by '\n' will be grouped into &lt;p&gt; elements.
     */
    private static final int TO_HTML_PARAGRAPH_FLAG = 0x00000001;

    private Html() { }

    /**
     * Returns displayable styled text from the provided HTML string with the legacy flags
     * {@link #FROM_HTML_MODE_LEGACY}.
     *
     * @deprecated use {@link #fromHtml(String, int)} instead.
     */
    @Deprecated
    public static Spanned fromHtml(String source) {
        return fromHtml(source, FROM_HTML_MODE_LEGACY, null, null);
    }

    /**
     * Returns displayable styled text from the provided HTML string. Any &lt;img&gt; tags in the
     * HTML will display as a generic replacement image which your program can then go through and
     * replace with real images.
     *
     * <p>This uses TagSoup to handle real HTML, including all of the brokenness found in the wild.
     */
    public static Spanned fromHtml(String source, int flags) {
        return fromHtml(source, flags, null, null);
    }

    /**
     * Lazy initialization holder for HTML parser. This class will
     * a) be preloaded by the zygote, or b) not loaded until absolutely
     * necessary.
     */
    private static class HtmlParser {
        private static final HTMLSchema schema = new HTMLSchema();
    }

    /**
     * Returns displayable styled text from the provided HTML string with the legacy flags
     * {@link #FROM_HTML_MODE_LEGACY}.
     *
     * @deprecated use {@link #fromHtml(String, int, Html.ImageGetter, Html.TagHandler)} instead.
     */
    @Deprecated
    public static Spanned fromHtml(String source, Html.ImageGetter imageGetter, Html.TagHandler tagHandler) {
        return fromHtml(source, FROM_HTML_MODE_LEGACY, imageGetter, tagHandler);
    }

    /**
     * Returns displayable styled text from the provided HTML string. Any &lt;img&gt; tags in the
     * HTML will use the specified ImageGetter to request a representation of the image (use null
     * if you don't want this) and the specified TagHandler to handle unknown tags (specify null if
     * you don't want this).
     *
     * <p>This uses TagSoup to handle real HTML, including all of the brokenness found in the wild.
     */
    public static Spanned fromHtml(String source, int flags, Html.ImageGetter imageGetter,
                                   Html.TagHandler tagHandler) {
        Parser parser = new Parser();
        try {
            parser.setProperty(Parser.schemaProperty, HtmlParser.schema);
        } catch (org.xml.sax.SAXNotRecognizedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        } catch (org.xml.sax.SAXNotSupportedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        }

        HtmlToSpannedConverter converter =
                new HtmlToSpannedConverter(source, imageGetter, tagHandler, parser, flags);
        return converter.convert();
    }

    /**
     * @deprecated use {@link #toHtml(Spanned, int)} instead.
     */
    @Deprecated
    public static String toHtml(Spanned text) {
        return toHtml(text, TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
    }

    /**
     * Returns an HTML representation of the provided Spanned text. A best effort is
     * made to add HTML tags corresponding to spans. Also note that HTML metacharacters
     * (such as "&lt;" and "&amp;") within the input text are escaped.
     *
     * @param text input text to convert
     * @param option one of {@link #TO_HTML_PARAGRAPH_LINES_CONSECUTIVE} or
     *     {@link #TO_HTML_PARAGRAPH_LINES_INDIVIDUAL}
     * @return string containing input converted to HTML
     */
    public static String toHtml(Spanned text, int option) {
        StringBuilder out = new StringBuilder();

        ///[UPGRADE#android.text.Html]
//        withinHtml(out, text, option);
        handleHtml(out, text, null);

        return out.toString();
    }

    /**
     * Returns an HTML escaped representation of the given plain text.
     */
    public static String escapeHtml(CharSequence text) {
        StringBuilder out = new StringBuilder();
        withinStyle(out, text, 0, text.length());
        return out.toString();
    }


    /* ------------------- ///[UPGRADE#android.text.Html] ------------------- */
    private static void handleHtml(StringBuilder out, Spanned text, ParagraphStyle compareSpan) {
        int start, end;
        if (compareSpan == null) {
            start = 0;
            end = text.length();
        } else {
            start = text.getSpanStart(compareSpan);
            end = text.getSpanEnd(compareSpan);
        }

        int next;
        for (int i = start; i < end; i = next) {
            final ParagraphStyle nextParagraphStyleSpan = getNextParagraphStyleSpan(text, i, compareSpan);

            if (nextParagraphStyleSpan == null) {
                next = text.nextSpanTransition(i, end, ParagraphStyle.class);

                handleParagraph(out, text, i, next, compareSpan == null
                        || compareSpan instanceof NestSpan && !(compareSpan instanceof ListItemSpan));
            } else {
                next = text.getSpanEnd(nextParagraphStyleSpan);

                if (nextParagraphStyleSpan instanceof DivSpan) {
                    out.append("<div>\n");
                } else
                if (nextParagraphStyleSpan instanceof CustomLeadingMarginSpan) {
                    final int leadingMarginSpanIndent = ((CustomLeadingMarginSpan) nextParagraphStyleSpan).getLeadingMargin(true);
                    out.append("<div style=\"text-indent:").append(leadingMarginSpanIndent).append("px;\">\n");
                } else
                if (nextParagraphStyleSpan instanceof AlignNormalSpan) {
                    out.append("<div style=\"text-align:start;\">\n");
                } else
                if (nextParagraphStyleSpan instanceof AlignCenterSpan) {
                    out.append("<div style=\"text-align:center;\">\n");
                } else
                if (nextParagraphStyleSpan instanceof AlignOppositeSpan) {
                    out.append("<div style=\"text-align:end;\">\n");
                } else
                if (nextParagraphStyleSpan instanceof ListSpan) {
                    final int listStart = ((ListSpan) nextParagraphStyleSpan).getStart();
                    final boolean isReversed = ((ListSpan) nextParagraphStyleSpan).isReversed();
                    final int listType = ((ListSpan) nextParagraphStyleSpan).getListType();

                    if (isListTypeOrdered(listType)) {
                        out.append("<ol start=\"").append(listStart).append("\"");

                        if (isReversed) {
                            out.append(" reversed");
                        }

                        if (listType == LIST_TYPE_ORDERED_DECIMAL) {
                            out.append(" style=\"list-style-type:decimal\"");
                        } else if (listType == LIST_TYPE_ORDERED_LOWER_LATIN) {
                            out.append(" style=\"list-style-type:lower-alpha\"");
                        } else if (listType == LIST_TYPE_ORDERED_UPPER_LATIN) {
                            out.append(" style=\"list-style-type:upper-alpha\"");
                        } else if (listType == LIST_TYPE_ORDERED_LOWER_ROMAN) {
                            out.append(" style=\"list-style-type:lower-roman\"");
                        } else if (listType == LIST_TYPE_ORDERED_UPPER_ROMAN) {
                            out.append(" style=\"list-style-type:upper-roman\"");
                        }

                        out.append(">\n");
                    } else {
                        out.append("<ul");

                        if (listType == LIST_TYPE_UNORDERED_DISC) {
                            out.append(" style=\"list-style-type:disc\"");
                        } else if (listType == LIST_TYPE_UNORDERED_CIRCLE) {
                            out.append(" style=\"list-style-type:circle\"");
                        } else if (listType == LIST_TYPE_UNORDERED_SQUARE) {
                            out.append(" style=\"list-style-type:square\"");
                        }

                        out.append(">\n");
                    }
                } else
                if (nextParagraphStyleSpan instanceof ListItemSpan) {
                    out.append("<li>\n");
                } else
                if (nextParagraphStyleSpan instanceof CustomQuoteSpan) {
                    out.append("<blockquote>\n");
                } else

                if (nextParagraphStyleSpan instanceof HeadSpan) {
                    out.append("<h").append(((HeadSpan) nextParagraphStyleSpan).getLevel() + 1).append(">");
                } else
                if (nextParagraphStyleSpan instanceof LineDividerSpan) {
                    out.append("<hr>\n");
                }

                handleHtml(out, text, nextParagraphStyleSpan);

                if (nextParagraphStyleSpan instanceof DivSpan
                        || nextParagraphStyleSpan instanceof CustomLeadingMarginSpan
                        || nextParagraphStyleSpan instanceof AlignNormalSpan
                        || nextParagraphStyleSpan instanceof AlignCenterSpan
                        || nextParagraphStyleSpan instanceof AlignOppositeSpan) {
                    out.append("</div>\n");
                } else
                if (nextParagraphStyleSpan instanceof ListSpan) {
                    final int listType = ((ListSpan) nextParagraphStyleSpan).getListType();
                    final String listTag = isListTypeOrdered(listType) ? "ol" : "ul";

                    out.append("</").append(listTag).append(">\n");
                } else
                if (nextParagraphStyleSpan instanceof ListItemSpan) {
                    out.append("</li>\n");
                } else
                if (nextParagraphStyleSpan instanceof CustomQuoteSpan) {
                    out.append("</blockquote>\n");
                } else

                if (nextParagraphStyleSpan instanceof HeadSpan) {
                    out.append("</h").append(((HeadSpan) nextParagraphStyleSpan).getLevel() + 1).append(">\n");
                }
            }
        }
    }

    private static ParagraphStyle getNextParagraphStyleSpan(Spanned text, int where, ParagraphStyle compareSpan) {
        ParagraphStyle resultSpan = null;

        final ArrayList<ParagraphStyle> paragraphStyleSpans = SpanUtil.getFilteredSpans(ParagraphStyle.class, (Editable) text, where, where, true);
        for (ParagraphStyle paragraphStyleSpan : paragraphStyleSpans) {
            if (paragraphStyleSpan == compareSpan) {
                break;
            }

            final int paragraphStyleSpanStart = text.getSpanStart(paragraphStyleSpan);
            final int paragraphStyleSpanEnd = text.getSpanEnd(paragraphStyleSpan);
            final int resultSpanEnd = text.getSpanEnd(resultSpan);
            if (paragraphStyleSpanStart == where && (resultSpan == null || resultSpanEnd <= paragraphStyleSpanEnd)) {
                resultSpan = paragraphStyleSpan;
            }
        }

        return resultSpan;
    }

    private static void handleParagraph(StringBuilder out, Spanned text, int start, int end, boolean isOutParagraph) {
        if (isOutParagraph) {
            out.append("<p").append(getTextDirection(text, start, end)).append(">");
        }

        int next;
        for (int i = start; i < end; i = next) {
            next = TextUtils.indexOf(text, '\n', i, end);
            if (next < 0) {
                next = end;
            }

            withinParagraph(out, text, i, next);

            if (++next < end) {
                if (isOutParagraph) {
                    out.append("</p>\n<p").append(getTextDirection(text, start, end)).append(">");
                } else {
                    out.append("<br>\n");
                }
            }
        }

        if (isOutParagraph) {
            out.append("</p>\n");
        } else {
            out.append('\n');
        }
    }

//
//    private static void withinHtml(StringBuilder out, Spanned text, int option) {
//        if ((option & TO_HTML_PARAGRAPH_FLAG) == TO_HTML_PARAGRAPH_LINES_CONSECUTIVE) {
//            encodeTextAlignmentByDiv(out, text, option);
//            return;
//        }
//
//        withinDiv(out, text, 0, text.length(), option);
//    }
//
//    private static void encodeTextAlignmentByDiv(StringBuilder out, Spanned text, int option) {
//        int len = text.length();
//
//        int next;
//        for (int i = 0; i < len; i = next) {
//            next = text.nextSpanTransition(i, len, ParagraphStyle.class);
//            ParagraphStyle[] style = text.getSpans(i, next, ParagraphStyle.class);
//            String elements = " ";
//            boolean needDiv = false;
//
//            for(int j = 0; j < style.length; j++) {
//                if (style[j] instanceof AlignmentSpan) {
//                    ///Error: The align attribute on the div element is obsolete. Use CSS instead./////////////////////?????????????????style="text-align:center"
//                    ///[UPGRADE#android.text.Html]
////                    Layout.Alignment align =
////                            ((AlignmentSpan) style[j]).getAlignment();
//                    needDiv = true;
////                    if (align == Layout.Alignment.ALIGN_CENTER) { ///[UPGRADE#android.text.Html]
//                    if (style[j] instanceof AlignCenterSpan) {
//                        elements = "align=\"center\" " + elements;
////                    } else if (align == Layout.Alignment.ALIGN_OPPOSITE) {    ///[UPGRADE#android.text.Html]
//                    } else if (style[j] instanceof AlignOppositeSpan) {
//                        elements = "align=\"right\" " + elements;
//                    } else {
//                        elements = "align=\"left\" " + elements;
//                    }
//                }
//            }
//            if (needDiv) {
//                out.append("<div ").append(elements).append(">");
//            }
//
//            withinDiv(out, text, i, next, option);
//
//            if (needDiv) {
//                out.append("</div>");
//            }
//        }
//    }
//
//    private static void withinDiv(StringBuilder out, Spanned text, int start, int end,
//                                  int option) {
//        int next;
//        for (int i = start; i < end; i = next) {
//            next = text.nextSpanTransition(i, end, CustomQuoteSpan.class);  ///[UPGRADE#android.text.Html]CustomQuoteSpan
//            CustomQuoteSpan[] quotes = text.getSpans(i, next, CustomQuoteSpan.class);   ///[UPGRADE#android.text.Html]CustomQuoteSpan
//
//            for (CustomQuoteSpan quote : quotes) {  ///[UPGRADE#android.text.Html]CustomQuoteSpan
//                out.append("<blockquote>");
//            }
//
//            withinBlockquote(out, text, i, next, option);
//
//            for (CustomQuoteSpan quote : quotes) {  ///[UPGRADE#android.text.Html]CustomQuoteSpan
//                out.append("</blockquote>\n");
//            }
//        }
//    }

    private static String getTextDirection(Spanned text, int start, int end) {
        ///[UPGRADE#android.text.Html]
//        if (TextDirectionHeuristics.FIRSTSTRONG_LTR.isRtl(text, start, end - start)) {
        if (TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR.isRtl(text, start, end - start)) {
            return " dir=\"rtl\"";
        } else {
            return " dir=\"ltr\"";
        }
    }

//    private static String getTextStyles(Spanned text, int start, int end,
//                                        boolean forceNoVerticalMargin, boolean includeTextAlign) {
//        String margin = null;
//        String textAlign = null;
//
//        if (forceNoVerticalMargin) {
//            margin = "margin-top:0; margin-bottom:0;";
//        }
//        if (includeTextAlign) {
//            final AlignmentSpan[] alignmentSpans = text.getSpans(start, end, AlignmentSpan.class);
//
//            // Only use the last AlignmentSpan with flag SPAN_PARAGRAPH
//            for (int i = alignmentSpans.length - 1; i >= 0; i--) {
//                AlignmentSpan s = alignmentSpans[i];
//                if ((text.getSpanFlags(s) & Spanned.SPAN_PARAGRAPH) == Spanned.SPAN_PARAGRAPH) {
//                    final Layout.Alignment alignment = s.getAlignment();
////                    if (alignment == Layout.Alignment.ALIGN_NORMAL) { ///[UPGRADE#android.text.Html]
//                    if (s instanceof AlignNormalSpan) {
//                        textAlign = "text-align:start;";
////                    } else if (alignment == Layout.Alignment.ALIGN_CENTER) {  ///[UPGRADE#android.text.Html]
//                    } else if (s instanceof AlignCenterSpan) {
//                        textAlign = "text-align:center;";
////                    } else if (alignment == Layout.Alignment.ALIGN_OPPOSITE) {    ///[UPGRADE#android.text.Html]
//                    } else if (s instanceof AlignOppositeSpan) {
//                        textAlign = "text-align:end;";
//                    }
//                    break;
//                }
//            }
//        }
//
//        if (margin == null && textAlign == null) {
//            return "";
//        }
//
//        final StringBuilder style = new StringBuilder(" style=\"");
//        if (margin != null && textAlign != null) {
//            style.append(margin).append(" ").append(textAlign);
//        } else if (margin != null) {
//            style.append(margin);
//        } else if (textAlign != null) {
//            style.append(textAlign);
//        }
//
//        return style.append("\"").toString();
//    }

//    private static void withinBlockquote(StringBuilder out, Spanned text, int start, int end,
//                                         int option) {
//        if ((option & TO_HTML_PARAGRAPH_FLAG) == TO_HTML_PARAGRAPH_LINES_CONSECUTIVE) {
//            withinBlockquoteConsecutive(out, text, start, end);
//        } else {
//            withinBlockquoteIndividual(out, text, start, end);
//        }
//    }
//
//    private static void withinBlockquoteIndividual(StringBuilder out, Spanned text, int start,
//                                                   int end) {
//        boolean isInList = false;
//        int next;
//        for (int i = start; i <= end; i = next) {
//            next = TextUtils.indexOf(text, '\n', i, end);
//            if (next < 0) {
//                next = end;
//            }
//
//            if (next == i) {
//                if (isInList) {
//                    // Current paragraph is no longer a list item; close the previously opened list
//                    isInList = false;
//                    out.append("</ul>\n");
//                }
//                out.append("<br>\n");
//            } else {
//                boolean isListItem = false;
//                ParagraphStyle[] paragraphStyles = text.getSpans(i, next, ParagraphStyle.class);
//                for (ParagraphStyle paragraphStyle : paragraphStyles) {
//                    ///////////////////
////                    final int spanFlags = text.getSpanFlags(paragraphStyle);
////                    if ((spanFlags & Spanned.SPAN_PARAGRAPH) == Spanned.SPAN_PARAGRAPH
////                            && paragraphStyle instanceof BulletSpan) {
//                    if (paragraphStyle instanceof ListSpan) {
//                        isListItem = true;
//                        break;
//                    }
//                }
//
//                if (isListItem && !isInList) {
//                    // Current paragraph is the first item in a list
//                    isInList = true;
//                    out.append("<ul")
//                            .append(getTextStyles(text, i, next, true, false))
//                            .append(">\n");
//                }
//
//                if (isInList && !isListItem) {
//                    // Current paragraph is no longer a list item; close the previously opened list
//                    isInList = false;
//                    out.append("</ul>\n");
//                }
//
//                String tagType = isListItem ? "li" : "p";
//                out.append("<").append(tagType)
//                        .append(getTextDirection(text, i, next))
//                        .append(getTextStyles(text, i, next, !isListItem, true))
//                        .append(">");
//
//                withinParagraph(out, text, i, next);
//
//                out.append("</");
//                out.append(tagType);
//                out.append(">\n");
//
//                if (next == end && isInList) {
//                    isInList = false;
//                    out.append("</ul>\n");
//                }
//            }
//
//            next++;
//        }
//    }
//
//    private static void withinBlockquoteConsecutive(StringBuilder out, Spanned text, int start,
//                                                    int end) {
//        out.append("<p").append(getTextDirection(text, start, end)).append(">");
//
//        int next;
//        for (int i = start; i < end; i = next) {
//            next = TextUtils.indexOf(text, '\n', i, end);
//            if (next < 0) {
//                next = end;
//            }
//
//            int nl = 0;
//
//            while (next < end && text.charAt(next) == '\n') {
//                nl++;
//                next++;
//            }
//
//            withinParagraph(out, text, i, next - nl);
//
//            if (nl == 1) {
//                out.append("<br>\n");
//            } else {
//                for (int j = 2; j < nl; j++) {
//                    out.append("<br>");
//                }
//                if (next != end) {
//                    /* Paragraph should be closed and reopened */
//                    out.append("</p>\n");
//                    out.append("<p").append(getTextDirection(text, start, end)).append(">");
//                }
//            }
//        }
//
//        out.append("</p>\n");
//    }

    private static void withinParagraph(StringBuilder out, Spanned text, int start, int end) {
        int next;
        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, CharacterStyle.class);
            CharacterStyle[] style = text.getSpans(i, next, CharacterStyle.class);

            for (int j = 0; j < style.length; j++) {
                if (style[j] instanceof StyleSpan) {
                    int s = ((StyleSpan) style[j]).getStyle();

                    if ((s & Typeface.BOLD) != 0) {
                        out.append("<b>");
                    }
                    if ((s & Typeface.ITALIC) != 0) {
                        out.append("<i>");
                    }
                } else

                if (style[j] instanceof UnderlineSpan) {
                    out.append("<u>");
                } else

                if (style[j] instanceof StrikethroughSpan) {
                    ///[UPGRADE#android.text.Html]
                    out.append("<span style=\"text-decoration:line-through;\">");
//                    out.append("<strike>");
                } else

                if (style[j] instanceof SuperscriptSpan) {
                    out.append("<sup>");
                } else
                if (style[j] instanceof SubscriptSpan) {
                    out.append("<sub>");
                } else

                if (style[j] instanceof CustomForegroundColorSpan) {
                    int color = ((CustomForegroundColorSpan) style[j]).getForegroundColor();
                    out.append(String.format("<span style=\"color:#%06X;\">", 0xFFFFFF & color));
                } else
                if (style[j] instanceof CustomBackgroundColorSpan) {
                    int color = ((CustomBackgroundColorSpan) style[j]).getBackgroundColor();
                    out.append(String.format("<span style=\"background-color:#%06X;\">",
                            0xFFFFFF & color));
                } else

                if (style[j] instanceof CustomFontFamilySpan) {
                    String s = ((CustomFontFamilySpan) style[j]).getFamily();
//                    ///注意：当face="monospace"时转换为tt标签
//                    if ("monospace".equals(s)) {
//                        out.append("<tt>");
//                    } else {
                        out.append("<font face=\"").append(s).append("\">");
//                    }
                } else

                if (style[j] instanceof CustomAbsoluteSizeSpan) {
                    CustomAbsoluteSizeSpan s = ((CustomAbsoluteSizeSpan) style[j]);
                    float sizeDip = s.getSize();
                    if (!s.getDip()) {
                        ///[UPGRADE#android.text.Html]px in CSS is the equivalance of dip in Android
                        ///注意：一般情况下，CustomAbsoluteSizeSpan的dip都为true，否则需要在使用Html之前设置本机的具体准确的屏幕密度！
//                        Application application = ActivityThread.currentApplication();
//                        sizeDip /= application.getResources().getDisplayMetrics().density;
                        sizeDip /= sDisplayMetricsDensity;
                    }

                    // px in CSS is the equivalance of dip in Android
                    out.append(String.format("<span style=\"font-size:%.0fpx\";>", sizeDip));
                } else
                if (style[j] instanceof CustomRelativeSizeSpan) {
                    float sizeEm = ((CustomRelativeSizeSpan) style[j]).getSizeChange();
//                    if (sizeEm == 1.25f) {
//                        out.append("<big>");
//                    } else if (sizeEm == 0.8f) {
//                        out.append("<small>");
//                    } else {
                        out.append(String.format("<span style=\"font-size:%.2fem;\">", sizeEm));
//                    }
                } else

                if (style[j] instanceof URLSpan) {
                    out.append("<a href=\"");
                    out.append(((URLSpan) style[j]).getURL());
                    out.append("\">");
                } else

                if (style[j] instanceof CustomImageSpan) {
                    if (style[j] instanceof VideoSpan) {
                        out.append("<video src=\"").append(((VideoSpan) style[j]).getUri()).append("\"");
                        out.append(" img=\"").append(((VideoSpan) style[j]).getSource()).append("\"");
                    } else if (style[j] instanceof AudioSpan) {
                        out.append("<audio src=\"").append(((AudioSpan) style[j]).getUri()).append("\"");
                        out.append(" img=\"").append(((AudioSpan) style[j]).getSource()).append("\"");
                    } else {
                        out.append("<img src=\"").append(((CustomImageSpan) style[j]).getSource()).append("\"");
                    }
                    out.append(" width=\"").append(((CustomImageSpan) style[j]).getDrawableWidth()).append("\"");
                    out.append(" height=\"").append(((CustomImageSpan) style[j]).getDrawableHeight()).append("\"");
                    out.append(" align=\"").append(((CustomImageSpan) style[j]).getVerticalAlignment()).append("\"");
                    out.append(">");

                    // Don't output the dummy character underlying the image.
                    i = next;
                }
            }

            withinStyle(out, text, i, next);

            for (int j = style.length - 1; j >= 0; j--) {
                if (style[j] instanceof StyleSpan) {
                    int s = ((StyleSpan) style[j]).getStyle();

                    if ((s & Typeface.BOLD) != 0) {
                        out.append("</b>");
                    }
                    if ((s & Typeface.ITALIC) != 0) {
                        out.append("</i>");
                    }
                } else

                if (style[j] instanceof UnderlineSpan) {
                    out.append("</u>");
                } else

                if (style[j] instanceof StrikethroughSpan) {
                    out.append("</span>");
//                    out.append("</strike>");
                } else

                if (style[j] instanceof SubscriptSpan) {
                    out.append("</sub>");
                } else
                if (style[j] instanceof SuperscriptSpan) {
                    out.append("</sup>");
                } else

                if (style[j] instanceof CustomBackgroundColorSpan || style[j] instanceof CustomForegroundColorSpan) {
                    out.append("</span>");
                } else

                if (style[j] instanceof CustomFontFamilySpan) {
                    String s = ((CustomFontFamilySpan) style[j]).getFamily();
//                    ///注意：当face="monospace"时转换为tt标签
//                    if ("monospace".equals(s)) {
//                        out.append("</tt>");
//                    } else {
                        out.append("</font>");
//                    }
                } else

                if (style[j] instanceof CustomAbsoluteSizeSpan) {
                    out.append("</span>");
                } else
                if (style[j] instanceof CustomRelativeSizeSpan) {
//                    float sizeEm = ((CustomRelativeSizeSpan) style[j]).getSizeChange();
//                    if (sizeEm == 1.25f) {
//                        out.append("</big>");
//                    } else if (sizeEm == 0.8f) {
//                        out.append("</small>");
//                    } else {
                        out.append("</span>");
//                    }
                } else

                if (style[j] instanceof URLSpan) {
                    out.append("</a>");
                }
            }
        }
    }

    private static void withinStyle(StringBuilder out, CharSequence text,
                                    int start, int end) {
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);

            if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '&') {
                out.append("&amp;");
            } else if (c >= 0xD800 && c <= 0xDFFF) {
                if (c < 0xDC00 && i + 1 < end) {
                    char d = text.charAt(i + 1);
                    if (d >= 0xDC00 && d <= 0xDFFF) {
                        i++;
                        int codepoint = 0x010000 | (int) c - 0xD800 << 10 | (int) d - 0xDC00;
                        out.append("&#").append(codepoint).append(";");
                    }
                }
            } else if (c > 0x7E || c < ' ') {
                out.append("&#").append((int) c).append(";");
            } else if (c == ' ') {
                while (i + 1 < end && text.charAt(i + 1) == ' ') {
                    out.append("&nbsp;");
                    i++;
                }

                out.append(' ');
            } else {
                out.append(c);
            }
        }
    }
}



class HtmlToSpannedConverter implements ContentHandler {

//    private static final float[] HEADING_SIZES = {
//            1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f,
//    };

    private String mSource;
    private XMLReader mReader;
    private SpannableStringBuilder mSpannableStringBuilder;
    private Html.ImageGetter mImageGetter;
    private Html.TagHandler mTagHandler;
    private int mFlags;

    private static Pattern sLeadingMargin;
    private static Pattern sTextAlignPattern;
    private static Pattern sForegroundColorPattern;
    private static Pattern sBackgroundColorPattern;
    private static Pattern sTextDecorationPattern;

    /**
     * Name-value mapping of HTML/CSS colors which have different values in {@link Color}.
     */
    private static final Map<String, Integer> sColorMap;

    static {
        sColorMap = new HashMap<>();
        sColorMap.put("darkgray", 0xFFA9A9A9);
        sColorMap.put("gray", 0xFF808080);
        sColorMap.put("lightgray", 0xFFD3D3D3);
        sColorMap.put("darkgrey", 0xFFA9A9A9);
        sColorMap.put("grey", 0xFF808080);
        sColorMap.put("lightgrey", 0xFFD3D3D3);
        sColorMap.put("green", 0xFF008000);
//        //////??????[UPGRADE#增加颜色]参考Color#sColorNameMap
//        sColorMap.put("red", 0xFFFF0000);
//        sColorMap.put("yellow", 0xFFFFFF00);
//        sColorMap.put("blue", 0xFF0000FF);
    }

    private static Pattern getLeadingMarginPattern() {
        if (sLeadingMargin == null) {
            sLeadingMargin = Pattern.compile("(?:\\s+|\\A)text-indent\\s*:\\s*(\\S*)\\b");
        }
        return sLeadingMargin;
    }

    private static Pattern getTextAlignPattern() {
        if (sTextAlignPattern == null) {
            sTextAlignPattern = Pattern.compile("(?:\\s+|\\A)text-align\\s*:\\s*(\\S*)\\b");
        }
        return sTextAlignPattern;
    }

    private static Pattern getForegroundColorPattern() {
        if (sForegroundColorPattern == null) {
            sForegroundColorPattern = Pattern.compile(
                    "(?:\\s+|\\A)color\\s*:\\s*(\\S*)\\b");
        }
        return sForegroundColorPattern;
    }

    private static Pattern getBackgroundColorPattern() {
        if (sBackgroundColorPattern == null) {
            sBackgroundColorPattern = Pattern.compile(
                    "(?:\\s+|\\A)background(?:-color)?\\s*:\\s*(\\S*)\\b");
        }
        return sBackgroundColorPattern;
    }

    private static Pattern getTextDecorationPattern() {
        if (sTextDecorationPattern == null) {
            sTextDecorationPattern = Pattern.compile(
                    "(?:\\s+|\\A)text-decoration\\s*:\\s*(\\S*)\\b");
        }
        return sTextDecorationPattern;
    }

    private int getHtmlColor(String color) {
        if ((mFlags & Html.FROM_HTML_OPTION_USE_CSS_COLORS)
                == Html.FROM_HTML_OPTION_USE_CSS_COLORS) {
            Integer i = sColorMap.get(color.toLowerCase(Locale.US));
            if (i != null) {
                return i;
            }
        }
        ///[UPGRADE#android.text.Html]
//        return Color.getHtmlColor(color);
        return Color.parseColor(color);
    }

    public HtmlToSpannedConverter(String source, Html.ImageGetter imageGetter,
                                  Html.TagHandler tagHandler, Parser parser, int flags) {
        mSource = source;
        mSpannableStringBuilder = new SpannableStringBuilder();
        mImageGetter = imageGetter;
        mTagHandler = tagHandler;
        mReader = parser;
        mFlags = flags;
    }

    public void setDocumentLocator(Locator locator) {}

    public void startDocument() throws SAXException {}

    public void endDocument() throws SAXException {}

    public void startPrefixMapping(String prefix, String uri) throws SAXException {}

    public void endPrefixMapping(String prefix) throws SAXException {}

    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        handleStartTag(localName, attributes);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        handleEndTag(localName);
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        handleCharacters(ch, start, length);
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {}

    public void processingInstruction(String target, String data) throws SAXException {}

    public void skippedEntity(String name) throws SAXException {}


    /* ---------------------------------------------------------------------------------- */
    public Spanned convert() {
        mReader.setContentHandler(this);
        try {
            mReader.parse(new InputSource(new StringReader(mSource)));
        } catch (IOException e) {
            // We are reading from a string. There should not be IO problems.
            throw new RuntimeException(e);
        } catch (SAXException e) {
            // TagSoup doesn't throw parse exceptions.
            throw new RuntimeException(e);
        }

        ///////////////？？？？？？？？？？？？？？？？？
//        // Fix flags and range for paragraph-type markup.
//        Object[] obj = mSpannableStringBuilder.getSpans(0, mSpannableStringBuilder.length(), ParagraphStyle.class);
//        for (int i = 0; i < obj.length; i++) {
//            int start = mSpannableStringBuilder.getSpanStart(obj[i]);
//            int end = mSpannableStringBuilder.getSpanEnd(obj[i]);
//
//            // If the last line of the range is blank, back off by one.
//            if (end - 2 >= 0) {
//                if (mSpannableStringBuilder.charAt(end - 1) == '\n' &&
//                        mSpannableStringBuilder.charAt(end - 2) == '\n') {
//                    end--;
//                }
//            }
//
//            if (end == start) {
//                mSpannableStringBuilder.removeSpan(obj[i]);
//            } else {
//                mSpannableStringBuilder.setSpan(obj[i], start, end, Spannable.SPAN_PARAGRAPH);
//            }
//        }

        return mSpannableStringBuilder;
    }

    private void handleStartTag(String tag, Attributes attributes) {
        if (tag.equalsIgnoreCase("br")) {
            // We don't need to handle this. TagSoup will ensure that there's a </br> for each <br>
            // so we can safely emit the linebreaks when we handle the close tag.

        } else if (tag.equalsIgnoreCase("p")) {
            startBlockElement(mSpannableStringBuilder, attributes);
            startCssStyle(mSpannableStringBuilder, attributes);

        } else if (tag.equalsIgnoreCase("div")) {
            final String styles = attributes.getValue("", "style");
            if (styles != null) {
                for (String style : styles.split(";")) {
                    Matcher m = getLeadingMarginPattern().matcher(style);
                    if (m.find()) {
                        int indent = 0;
                        String indentString = m.group(1).toLowerCase();
                        if (indentString.endsWith("px")) {
                            String i = indentString.substring(0, indentString.length() - 2);
                            if (isInteger(i)) {
                                indent = Integer.parseInt(i);
                            }
                        } else {
                            // todo ...
                        }
                        start(mSpannableStringBuilder, new LeadingMarginDiv(indent));
                    } else {
                        m = getTextAlignPattern().matcher(style);
                        if (m.find()) {
                            String alignment = m.group(1);
                            if (alignment.equalsIgnoreCase("center")) {
                                start(mSpannableStringBuilder, new AlignmentDiv(Layout.Alignment.ALIGN_CENTER));
                            } else if (alignment.equalsIgnoreCase("end")) {
                                start(mSpannableStringBuilder, new AlignmentDiv(Layout.Alignment.ALIGN_OPPOSITE));
                            } else {
                                start(mSpannableStringBuilder, new AlignmentDiv(Layout.Alignment.ALIGN_NORMAL));
                            }
                        } else {
                            start(mSpannableStringBuilder, new Div());
                        }
                    }
                }
            } else {
                start(mSpannableStringBuilder, new Div());
            }

        } else if (tag.equalsIgnoreCase("ul")) {
            startBlockElement(mSpannableStringBuilder, attributes);
        } else if (tag.equalsIgnoreCase("li")) {
            startLi(mSpannableStringBuilder, attributes);

        } else if (tag.equalsIgnoreCase("blockquote")) {
            startBlockquote(mSpannableStringBuilder, attributes);


        } else if (tag.length() == 2 &&
                Character.toLowerCase(tag.charAt(0)) == 'h' &&
                tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
            startHeading(mSpannableStringBuilder, attributes, tag.charAt(1) - '1');


        } else if (tag.equalsIgnoreCase("span")) {
            startCssStyle(mSpannableStringBuilder, attributes);

        } else if (tag.equalsIgnoreCase("strong")
                || tag.equalsIgnoreCase("b")) {
            start(mSpannableStringBuilder, new Bold());
        } else if (tag.equalsIgnoreCase("em")
                || tag.equalsIgnoreCase("cite")
                || tag.equalsIgnoreCase("dfn")
                || tag.equalsIgnoreCase("i")) {
            start(mSpannableStringBuilder, new Italic());
        } else if (tag.equalsIgnoreCase("u")) {
            start(mSpannableStringBuilder, new Underline());
        } else if (tag.equalsIgnoreCase("del")
                || tag.equalsIgnoreCase("s")
                || tag.equalsIgnoreCase("strike")) {
            start(mSpannableStringBuilder, new Strikethrough());
        } else if (tag.equalsIgnoreCase("sup")) {
            start(mSpannableStringBuilder, new Super());
        } else if (tag.equalsIgnoreCase("sub")) {
            start(mSpannableStringBuilder, new Sub());

        } else if (tag.equalsIgnoreCase("tt")) {
            start(mSpannableStringBuilder, new Monospace());
        } else if (tag.equalsIgnoreCase("font")) {
            startFont(mSpannableStringBuilder, attributes);

        } else if (tag.equalsIgnoreCase("big")) {
            start(mSpannableStringBuilder, new Big());
        } else if (tag.equalsIgnoreCase("small")) {
            start(mSpannableStringBuilder, new Small());

        } else if (tag.equalsIgnoreCase("a")) {
            startA(mSpannableStringBuilder, attributes);

        } else if (tag.equalsIgnoreCase("img")) {
            startImg(mSpannableStringBuilder, attributes, mImageGetter);

        } else if (mTagHandler != null) {
            mTagHandler.handleTag(true, tag, mSpannableStringBuilder, mReader);
        }
    }

    private void handleCharacters(char ch[], int start, int length) {
        StringBuilder sb = new StringBuilder();

        /*
         * Ignore whitespace that immediately follows other whitespace;
         * newlines count as spaces.
         */

        for (int i = 0; i < length; i++) {
            char c = ch[i + start];

            ///[UPGRADE#android.text.Html#'\n'应该忽略！]
//            if (c == ' ' && c == '\n') {
            if (c == ' ') {
                char pred;
                int len = sb.length();

                if (len == 0) {
                    len = mSpannableStringBuilder.length();

                    if (len == 0) {
                        pred = '\n';
                    } else {
                        pred = mSpannableStringBuilder.charAt(len - 1);
                    }
                } else {
                    pred = sb.charAt(len - 1);
                }

                ///[UPGRADE#android.text.Html#'\n'应该忽略！]
//                if (pred != ' ' && pred != '\n') {
                if (pred != ' ') {

                    sb.append(' ');
                }

                ///[UPGRADE#android.text.Html#'\n'应该忽略！]
//            } else {
            } else if (c != '\n') {

                sb.append(c);
            }
        }

        mSpannableStringBuilder.append(sb);
    }

    private void handleEndTag(String tag) {
        if (tag.equalsIgnoreCase("br")) {
            handleBr(mSpannableStringBuilder);

        } else if (tag.equalsIgnoreCase("p")) {
            endCssStyle(mSpannableStringBuilder);
            endBlockElement(mSpannableStringBuilder, null);

        } else if (tag.equalsIgnoreCase("div")) {
            ///[UPGRADE#android.text.Html#Div#LeadingMarginDiv/AlignmentDiv都继承Div]
            final Div d = getLast(mSpannableStringBuilder, Div.class);
            if (d instanceof LeadingMarginDiv) {
                endBlockElement(mSpannableStringBuilder, LeadingMarginDiv.class);
                final int nestingLevel = getNestingLevel(mSpannableStringBuilder, LeadingMarginDiv.class);
                end(mSpannableStringBuilder, LeadingMarginDiv.class, new CustomLeadingMarginSpan(nestingLevel, ((LeadingMarginDiv) d).mIndent));
            } else if (d instanceof AlignmentDiv) {
                endBlockElement(mSpannableStringBuilder, AlignmentDiv.class);
                final int nestingLevel = getNestingLevel(mSpannableStringBuilder, AlignmentDiv.class);
                if (((AlignmentDiv) d).mAlignment == Layout.Alignment.ALIGN_CENTER) {
                    end(mSpannableStringBuilder, AlignmentDiv.class, new AlignCenterSpan(nestingLevel));
                } else if (((AlignmentDiv) d).mAlignment == Layout.Alignment.ALIGN_OPPOSITE) {
                    end(mSpannableStringBuilder, AlignmentDiv.class, new AlignOppositeSpan(nestingLevel));
                } else {
                    end(mSpannableStringBuilder, AlignmentDiv.class, new AlignNormalSpan(nestingLevel));
                }
            } else {
                endBlockElement(mSpannableStringBuilder, Div.class);
                final int nestingLevel = getNestingLevel(mSpannableStringBuilder, Div.class);
                end(mSpannableStringBuilder, Div.class, new DivSpan(nestingLevel));
            }

        } else if (tag.equalsIgnoreCase("ul")) {
            endBlockElement(mSpannableStringBuilder, null);
        } else if (tag.equalsIgnoreCase("li")) {
            endLi(mSpannableStringBuilder);

        } else if (tag.equalsIgnoreCase("blockquote")) {
            endBlockquote(mSpannableStringBuilder);


        } else if (tag.length() == 2 &&
                Character.toLowerCase(tag.charAt(0)) == 'h' &&
                tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
            endHeading(mSpannableStringBuilder);


        } else if (tag.equalsIgnoreCase("span")) {
            endCssStyle(mSpannableStringBuilder);

        } else if (tag.equalsIgnoreCase("strong")
                || tag.equalsIgnoreCase("b")) {
            ///[UPGRADE#android.text.Html]
//            end(mSpannableStringBuilder, Bold.class, new StyleSpan(Typeface.BOLD));
            end(mSpannableStringBuilder, Bold.class, new BoldSpan());
        } else if (tag.equalsIgnoreCase("em")
                || tag.equalsIgnoreCase("cite")
                || tag.equalsIgnoreCase("dfn")
                || tag.equalsIgnoreCase("i")) {
            ///[UPGRADE#android.text.Html]
//            end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            end(mSpannableStringBuilder, Italic.class, new ItalicSpan());
        } else if (tag.equalsIgnoreCase("u")) {
            ///[UPGRADE#android.text.Html]
//            end(mSpannableStringBuilder, Underline.class, new UnderlineSpan());
            end(mSpannableStringBuilder, Underline.class, new CustomUnderlineSpan());
        } else if (tag.equalsIgnoreCase("del")
                || tag.equalsIgnoreCase("s")
                || tag.equalsIgnoreCase("strike")) {
            ///[UPGRADE#android.text.Html]
//            end(mSpannableStringBuilder, Strikethrough.class, new StrikethroughSpan());
            end(mSpannableStringBuilder, Strikethrough.class, new CustomStrikethroughSpan());
        } else if (tag.equalsIgnoreCase("sup")) {
            ///[UPGRADE#android.text.Html]
//            end(mSpannableStringBuilder, Super.class, new SuperscriptSpan());
            end(mSpannableStringBuilder, Super.class, new CustomSuperscriptSpan());
        } else if (tag.equalsIgnoreCase("sub")) {
            ///[UPGRADE#android.text.Html]
//            end(mSpannableStringBuilder, Sub.class, new SubscriptSpan());
            end(mSpannableStringBuilder, Sub.class, new CustomSubscriptSpan());
        } else if (tag.equalsIgnoreCase("tt")) {
//            end(mSpannableStringBuilder, Monospace.class, new TypefaceSpan("monospace"));
            end(mSpannableStringBuilder, Monospace.class, new CustomFontFamilySpan("monospace"));
        } else if (tag.equalsIgnoreCase("font")) {
            endFont(mSpannableStringBuilder);
        } else if (tag.equalsIgnoreCase("big")) {
            ///[UPGRADE#android.text.Html]
//            end(mSpannableStringBuilder, Big.class, new RelativeSizeSpan(1.25f));
            end(mSpannableStringBuilder, Big.class, new CustomRelativeSizeSpan(1.25f));
        } else if (tag.equalsIgnoreCase("small")) {
            ///[UPGRADE#android.text.Html]
//            end(mSpannableStringBuilder, Small.class, new RelativeSizeSpan(0.8f));
            end(mSpannableStringBuilder, Small.class, new CustomRelativeSizeSpan(0.8f));

        } else if (tag.equalsIgnoreCase("a")) {
            endA(mSpannableStringBuilder);

        } else if (mTagHandler != null) {
            mTagHandler.handleTag(false, tag, mSpannableStringBuilder, mReader);
        }
    }


    /* ----------------------------------------------------------------------------------------- */
    private static void handleBr(Editable text) {
        text.append('\n');
    }

    private void startLi(Editable text, Attributes attributes) {
        startBlockElement(text, attributes);
        start(text, new Bullet());
        startCssStyle(text, attributes);
    }

    private static void endLi(Editable text) {
        endCssStyle(text);
        endBlockElement(text, null);
        end(text, Bullet.class, new BulletSpan());
    }

    private void startBlockquote(Editable text, Attributes attributes) {
        startBlockElement(text, attributes);
        start(text, new Blockquote());
    }

    private static void endBlockquote(Editable text) {
        ///[UPGRADE#android.text.Html]
//        endBlockElement(text);
//        end(text, Blockquote.class, new QuoteSpan());
        endBlockElement(text, Blockquote.class);
        final int nestingLevel = getNestingLevel(text, Blockquote.class);
        end(text, Blockquote.class, new CustomQuoteSpan(nestingLevel));
    }

    private void startHeading(Editable text, Attributes attributes, int level) {
        startBlockElement(text, attributes);
        start(text, new Heading(level));
    }

    private static void endHeading(Editable text) {
        // RelativeSizeSpan and StyleSpan are CharacterStyles
        // Their ranges should not include the newlines at the end
        Heading h = getLast(text, Heading.class);
        if (h != null) {
            ///[UPGRADE#android.text.Html]
//            setSpanFromMark(text, h, new RelativeSizeSpan(HEADING_SIZES[h.mLevel]),
//                    new StyleSpan(Typeface.BOLD));
            setSpanFromMark(text, h, new HeadSpan(h.mLevel));
        }

        endBlockElement(text, null);
    }


    private void startFont(Editable text, Attributes attributes) {
        String color = attributes.getValue("", "color");
        String face = attributes.getValue("", "face");

        ///[UPGRADE#android.text.Html#Font增加尺寸size（px、%）]
        ///https://blog.csdn.net/qq_36009027/article/details/84371825
        String size = attributes.getValue("", "size");
        if (!TextUtils.isEmpty(size)) {
            if (size.contains("px")) {
                size = size.split("px")[0];
                ///[UPGRADE#android.text.Html]px in CSS is the equivalance of dip in Android
                ///注意：一般情况下，CustomAbsoluteSizeSpan的dip都为true，否则需要在使用Html之前设置本机的具体准确的屏幕密度！
                start(text, new AbsoluteSize(Integer.parseInt(size), true));
            } else if (size.contains("%")) {
                size = size.split("%")[0];
                start(text, new RelativeSize(Float.parseFloat(size) / 100));
            } else {
                // todo ...
            }
        }

        if (!TextUtils.isEmpty(color)) {
            int c = getHtmlColor(color);
            if (c != -1) {
                start(text, new Foreground(c | 0xFF000000));
            }
        }

        if (!TextUtils.isEmpty(face)) {
            start(text, new Font(face));
        }
    }

    private static void endFont(Editable text) {
        Font font = getLast(text, Font.class);
        if (font != null) {
            ///[UPGRADE#android.text.Html]
//            setSpanFromMark(text, font, new TypefaceSpan(font.mFace));
            setSpanFromMark(text, font, new CustomFontFamilySpan(font.mFace));
        }

        Foreground foreground = getLast(text, Foreground.class);
        if (foreground != null) {
            setSpanFromMark(text, foreground,
                    new CustomForegroundColorSpan(foreground.mForegroundColor));
        }

        ///[UPGRADE#android.text.Html#Font增加尺寸size（px、%）]
        ///https://blog.csdn.net/qq_36009027/article/details/84371825
        AbsoluteSize absoluteSize = getLast(text, AbsoluteSize.class);
        if (absoluteSize != null) {
            setSpanFromMark(text, absoluteSize,
                    new CustomAbsoluteSizeSpan(absoluteSize.mSize, absoluteSize.mDip));
        } else {
            RelativeSize relativeSize = getLast(text, RelativeSize.class);
            if (relativeSize != null) {
                setSpanFromMark(text, relativeSize,
                        new CustomRelativeSizeSpan(relativeSize.mProportion));
            } else {
                // todo ...
            }
        }
    }

    private static void startA(Editable text, Attributes attributes) {
        String href = attributes.getValue("", "href");
        start(text, new Href(href));
    }

    private static void endA(Editable text) {
        Href h = getLast(text, Href.class);
        if (h != null) {
            if (h.mHref != null) {
                ///[UPGRADE#android.text.Html]
//                setSpanFromMark(text, h, new URLSpan((h.mHref)));
                setSpanFromMark(text, h, new CustomURLSpan(h.mHref));
            }
        }
    }

    private static void startImg(Editable text, Attributes attributes, Html.ImageGetter img) {
        String src = attributes.getValue("", "src");
        Drawable d = null;

        if (img != null) {
            d = img.getDrawable(src);
        }

        if (d == null) {
            d = Resources.getSystem().
                    ///[UPGRADE#android.text.Html]Resources.getSystem() can only support system resources!
//                    getDrawable(com.android.internal.R.drawable.unknown_image);
//                    getDrawable(android.R.drawable.gallery_thumb);
        getDrawable(android.R.drawable.picture_frame);
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        }

        int len = text.length();
        text.append("\uFFFC");/////////??????????要按照RichEditorToolbar的要求添加[img uri= src= ]

        ///[UPGRADE#android.text.Html]
//        text.setSpan(new ImageSpan(d, src), len, text.length(),
        text.setSpan(new CustomImageSpan(d, src), len, text.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }


    /* ---------------------------------------------------------------------------------- */
    private static void startBlockElement(Editable text, Attributes attributes) {
        final String styles = attributes.getValue("", "style");
        if (styles != null) {
            for (String style : styles.split(";")) {
                if (style != null) {
                    Matcher m = getLeadingMarginPattern().matcher(style);
                    if (m.find()) {
                        int indent = 0;
                        String indentString = m.group(1).toLowerCase();
                        if (indentString.endsWith("px")) {
                            String i = indentString.substring(0, indentString.length() - 2);
                            if (isInteger(i)) {
                                indent = Integer.parseInt(i);
                            }
                        } else {
                            // todo ...
                        }
                        start(text, new LeadingMargin(indent));
                    }

                    m = getTextAlignPattern().matcher(style);
                    if (m.find()) {
                        String alignment = m.group(1);
                        if (alignment.equalsIgnoreCase("center")) {
                            start(text, new Alignment(Layout.Alignment.ALIGN_CENTER));
                        } else if (alignment.equalsIgnoreCase("end")) {
                            start(text, new Alignment(Layout.Alignment.ALIGN_OPPOSITE));
                        } else {
                            start(text, new Alignment(Layout.Alignment.ALIGN_NORMAL));
                        }
                    }
                }
            }
        }
    }

    private static void endBlockElement(Editable text, Class kind) {
        int len = text.length();
        if (kind == null || len == 0 || text.charAt(len - 1) != '\n') {
            text.append('\n');
        } else {
            ///注意：当kind不为null、且是嵌套而不是并列时（即其start大于等于text.length()），忽略添加'\n'
            Object obj = getLast(text, kind);
            if (obj == null || text.getSpanStart(obj) >= len) {
                text.append('\n');
            }
        }

        //////？？？？？？？？？？
//        if (kind != null && kind != Div.class && kind != LeadingMarginDiv.class && kind != AlignmentDiv.class) {
        if (kind != null) {
            Alignment a = getLast(text, Alignment.class);
            if (a != null) {
                final int nestingLevel = getNestingLevel(text, Alignment.class);
                if (a.mAlignment == Layout.Alignment.ALIGN_CENTER) {
                    setSpanFromMark(text, a, new AlignCenterSpan(nestingLevel));
                } else if (a.mAlignment == Layout.Alignment.ALIGN_OPPOSITE) {
                    setSpanFromMark(text, a, new AlignOppositeSpan(nestingLevel));
                } else {
                    setSpanFromMark(text, a, new AlignNormalSpan(nestingLevel));
                }
            }

            LeadingMargin l = getLast(text, LeadingMargin.class);
            if (l != null) {
                final int nestingLevel = getNestingLevel(text, LeadingMargin.class);
                setSpanFromMark(text, l, new CustomLeadingMarginSpan(nestingLevel, l.mIndent));
            }
        }
    }

    private void startCssStyle(Editable text, Attributes attributes) {
        String style = attributes.getValue("", "style");
        if (style != null) {
            Matcher m = getForegroundColorPattern().matcher(style);
            if (m.find()) {
                int c = getHtmlColor(m.group(1));
                if (c != -1) {
                    start(text, new Foreground(c | 0xFF000000));
                }
            }

            m = getBackgroundColorPattern().matcher(style);
            if (m.find()) {
                int c = getHtmlColor(m.group(1));
                if (c != -1) {
                    start(text, new Background(c | 0xFF000000));
                }
            }

            m = getTextDecorationPattern().matcher(style);
            if (m.find()) {
                String textDecoration = m.group(1);
                if (textDecoration.equalsIgnoreCase("line-through")) {
                    start(text, new Strikethrough());
                }
            }
        }
    }

    private static void endCssStyle(Editable text) {
        Strikethrough s = getLast(text, Strikethrough.class);
        if (s != null) {
            ///[UPGRADE#android.text.Html]
//            setSpanFromMark(text, s, new StrikethroughSpan());
            setSpanFromMark(text, s, new CustomStrikethroughSpan());
        }

        Background b = getLast(text, Background.class);
        if (b != null) {
            ///[UPGRADE#android.text.Html]
//            setSpanFromMark(text, b, new BackgroundColorSpan(b.mBackgroundColor));
            setSpanFromMark(text, b, new CustomBackgroundColorSpan(b.mBackgroundColor));
        }

        Foreground f = getLast(text, Foreground.class);
        if (f != null) {
            ///[UPGRADE#android.text.Html]
//            setSpanFromMark(text, f, new ForegroundColorSpan(f.mForegroundColor));
            setSpanFromMark(text, f, new CustomForegroundColorSpan(f.mForegroundColor));
        }
    }


    /* ---------------------------------------------------------------------------------- */
    private static void start(Editable text, Object mark) {
        int len = text.length();
        text.setSpan(mark, len, len, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    private static void end(Editable text, Class kind, Object repl) {
        Object obj = getLast(text, kind);
        if (obj != null) {
            setSpanFromMark(text, obj, repl);
        }
    }

    private static void setSpanFromMark(Spannable text, Object mark, Object... spans) {
        int where = text.getSpanStart(mark);
        text.removeSpan(mark);
        int len = text.length();
        if (where != len) {
            for (Object span : spans) {
                ///[UPGRADE#android.text.Html]
                ///注意：RichEditorToolbar要求BlockCharacterStyle为SPAN_EXCLUSIVE_EXCLUSIVE以外，
                // CharacterStyle为SPAN_INCLUSIVE_INCLUSIVE，其它都为SPAN_INCLUSIVE_EXCLUSIVE
//                text.setSpan(span, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setSpan(span, where, len, getSpanFlags(span.getClass()));
            }
        }
    }

    private static <T> T getLast(Spanned text, Class<T> kind) {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
        T[] objs = text.getSpans(0, text.length(), kind);

        if (objs.length == 0) {
            return null;
        } else {
            return objs[objs.length - 1];
        }
    }

    private static int getNestingLevel(Spanned text, Class kind) {
        return text.getSpans(0, text.length(), kind).length;
    }

    private static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    /* ---------------------------------------------------------------------------------- */
    ///[UPGRADE#android.text.Html#Div#LeadingMarginDiv/AlignmentDiv都继承Div]
    private static class Div { }
    private static class LeadingMarginDiv extends Div {
        public int mIndent;

        public LeadingMarginDiv(int indent) {
            mIndent = indent;
        }
    }
    private static class AlignmentDiv extends Div {
        private Layout.Alignment mAlignment;

        public AlignmentDiv(Layout.Alignment alignment) {
            mAlignment = alignment;
        }
    }
    private static class LeadingMargin extends Div {
        public int mIndent;

        public LeadingMargin(int indent) {
            mIndent = indent;
        }
    }

    private static class Alignment extends Div {
        private Layout.Alignment mAlignment;

        public Alignment(Layout.Alignment alignment) {
            mAlignment = alignment;
        }
    }

    private static class Blockquote { }
    private static class Bullet { }

    private static class Heading {
        private int mLevel;

        public Heading(int level) {
            mLevel = level;
        }
    }

    private static class Bold { }
    private static class Italic { }
    private static class Underline { }
    private static class Strikethrough { }
    private static class Super { }
    private static class Sub { }
    private static class Href {
        public String mHref;

        public Href(String href) {
            mHref = href;
        }
    }
    private static class Foreground {
        private int mForegroundColor;

        public Foreground(int foregroundColor) {
            mForegroundColor = foregroundColor;
        }
    }
    private static class Background {
        private int mBackgroundColor;

        public Background(int backgroundColor) {
            mBackgroundColor = backgroundColor;
        }
    }
    private static class Font {
        public String mFace;

        public Font(String face) {
            mFace = face;
        }
    }
    private static class Monospace { }
    private static class Big { }
    private static class Small { }
    ///[UPGRADE#android.text.Html#Font增加尺寸size（px、%）]
    ///https://blog.csdn.net/qq_36009027/article/details/84371825
    private static class AbsoluteSize {
        private int mSize;
        private boolean mDip;

        public AbsoluteSize(int size) {
            this(size, false);
        }
        public AbsoluteSize(int size, boolean dip) {
            mSize = size;
            mDip = dip;
        }
    }
    private static class RelativeSize {
        private float mProportion;

        public RelativeSize(float proportion) {
            mProportion = proportion;
        }
    }

}
