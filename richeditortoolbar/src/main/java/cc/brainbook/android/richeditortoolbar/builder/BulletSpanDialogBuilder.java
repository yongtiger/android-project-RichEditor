package cc.brainbook.android.richeditortoolbar.builder;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import cc.brainbook.android.colorpicker.ColorPickerView;
import cc.brainbook.android.colorpicker.OnColorChangedListener;
import cc.brainbook.android.colorpicker.OnColorSelectedListener;
import cc.brainbook.android.colorpicker.Utils;
import cc.brainbook.android.colorpicker.builder.ColorPickerClickListener;
import cc.brainbook.android.colorpicker.builder.ColorWheelRendererBuilder;
import cc.brainbook.android.colorpicker.renderer.ColorWheelRenderer;
import cc.brainbook.android.colorpicker.slider.AlphaSlider;
import cc.brainbook.android.colorpicker.slider.LightnessSlider;
import cc.brainbook.android.richeditortoolbar.R;

public class BulletSpanDialogBuilder {

	private int mBulletRadius;
	private int mGapWidth;
	private TextView mTextViewBulletRadius;
	private SeekBar mSeekBarBulletRadius;
	private TextView mTextViewGapWidth;
	private SeekBar mSeekBarGapWidth;

	private AlertDialog.Builder builder;
	private LinearLayout pickerContainer;
	private ColorPickerView colorPickerView;
	private LightnessSlider lightnessSlider;
	private AlphaSlider alphaSlider;
	private EditText colorEdit;
	private LinearLayout colorPreview;

	private boolean isLightnessSliderEnabled = true;
	private boolean isAlphaSliderEnabled = true;
	private boolean isBorderEnabled = true;
	private boolean isColorEditEnabled = false;
	private boolean isPreviewEnabled = false;
	private int pickerCount = 1;
	private int defaultMargin = 0;
	private int defaultMarginTop = 0;
	private Integer[] initialColor = new Integer[]{null, null, null, null, null};


	private BulletSpanDialogBuilder(Context context) {
		this(context, 0);
	}

	private BulletSpanDialogBuilder(Context context, int theme) {
		defaultMargin = getDimensionAsPx(context, cc.brainbook.android.colorpicker.R.dimen.default_slider_margin);
		defaultMarginTop = getDimensionAsPx(context, cc.brainbook.android.colorpicker.R.dimen.default_margin_top);

		colorPickerView = new ColorPickerView(context);

		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.layout_bullet_span_dialog, null);
		pickerContainer = layout.findViewById(R.id.picker_container);
		pickerContainer.addView(colorPickerView);

		builder = new AlertDialog.Builder(context, theme);
		builder.setView(layout);

		mTextViewBulletRadius = layout.findViewById(R.id.tv_bullet_radius);
		mSeekBarBulletRadius = layout.findViewById(R.id.sb_bullet_radius);
		mTextViewGapWidth = layout.findViewById(R.id.tv_gap_width);
		mSeekBarGapWidth = layout.findViewById(R.id.sb_gap_width);
		mSeekBarBulletRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBulletRadius = progress;
				mTextViewBulletRadius.setText(String.format(seekBar.getContext().getResources().getString(R.string.bullet_span_bullet_radius), progress));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
		mSeekBarGapWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mGapWidth = progress;
				mTextViewGapWidth.setText(String.format(seekBar.getContext().getResources().getString(R.string.bullet_span_gap_width), progress));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
	}

	public static BulletSpanDialogBuilder with(Context context) {
		return new BulletSpanDialogBuilder(context);
	}

	public static BulletSpanDialogBuilder with(Context context, int theme) {
		return new BulletSpanDialogBuilder(context, theme);
	}

	public BulletSpanDialogBuilder setTitle(String title) {
		builder.setTitle(title);
		return this;
	}

	public BulletSpanDialogBuilder setTitle(int titleId) {
		builder.setTitle(titleId);
		return this;
	}

	public BulletSpanDialogBuilder initialColor(int initialColor) {
		this.initialColor[0] = initialColor;
		return this;
	}

	public BulletSpanDialogBuilder initialColors(int[] initialColor) {
		for (int i = 0; i < initialColor.length && i < this.initialColor.length; i++) {
			this.initialColor[i] = initialColor[i];
		}

		return this;
	}

	public BulletSpanDialogBuilder initial(int initialColor, int bulletRadius, int gapWidth) {
		this.initialColor[0] = initialColor;

		mBulletRadius = bulletRadius;
		mGapWidth = gapWidth;

		return this;
	}

	public BulletSpanDialogBuilder wheelType(ColorPickerView.WHEEL_TYPE wheelType) {
		ColorWheelRenderer renderer = ColorWheelRendererBuilder.getRenderer(wheelType);
		colorPickerView.setRenderer(renderer);
		return this;
	}

	public BulletSpanDialogBuilder density(int density) {
		colorPickerView.setDensity(density);
		return this;
	}

	public BulletSpanDialogBuilder setOnColorChangedListener(OnColorChangedListener onColorChangedListener) {
		colorPickerView.addOnColorChangedListener(onColorChangedListener);
		return this;
	}

	public BulletSpanDialogBuilder setOnColorSelectedListener(OnColorSelectedListener onColorSelectedListener) {
		colorPickerView.addOnColorSelectedListener(onColorSelectedListener);
		return this;
	}

	public BulletSpanDialogBuilder setPositiveButton(CharSequence text, final ColorPickerClickListener onClickListener) {
		builder.setPositiveButton(text, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				positiveButtonOnClick(dialog, onClickListener);
			}
		});
		return this;
	}

	public BulletSpanDialogBuilder setPositiveButton(int textId, final ColorPickerClickListener onClickListener) {
		builder.setPositiveButton(textId, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				positiveButtonOnClick(dialog, onClickListener);
			}
		});
		return this;
	}

	public BulletSpanDialogBuilder setPositiveButton(CharSequence text, final PickerClickListener onClickListener) {
		builder.setPositiveButton(text, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int selectedColor = colorPickerView.getSelectedColor();
				Integer[] allColors = colorPickerView.getAllColors();
				onClickListener.onClick(dialog, selectedColor, allColors, mBulletRadius, mGapWidth);
			}
		});
		return this;
	}

	public BulletSpanDialogBuilder setPositiveButton(int textId, final PickerClickListener onClickListener) {
		builder.setPositiveButton(textId, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int selectedColor = colorPickerView.getSelectedColor();
				Integer[] allColors = colorPickerView.getAllColors();
				onClickListener.onClick(dialog, selectedColor, allColors, mBulletRadius, mGapWidth);
			}
		});
		return this;
	}

	public BulletSpanDialogBuilder setNegativeButton(CharSequence text, DialogInterface.OnClickListener onClickListener) {
		builder.setNegativeButton(text, onClickListener);
		return this;
	}

	public BulletSpanDialogBuilder setNegativeButton(int textId, DialogInterface.OnClickListener onClickListener) {
		builder.setNegativeButton(textId, onClickListener);
		return this;
	}

	///[UPGRADE#setNeutralButton()]
	public BulletSpanDialogBuilder setNeutralButton(int textId, DialogInterface.OnClickListener onClickListener) {
		builder.setNeutralButton(textId, onClickListener);
		return this;
	}
	public BulletSpanDialogBuilder setNeutralButton(CharSequence text, DialogInterface.OnClickListener onClickListener) {
		builder.setNeutralButton(text, onClickListener);
		return this;
	}

	public BulletSpanDialogBuilder noSliders() {
		isLightnessSliderEnabled = false;
		isAlphaSliderEnabled = false;
		return this;
	}

	public BulletSpanDialogBuilder alphaSliderOnly() {
		isLightnessSliderEnabled = false;
		isAlphaSliderEnabled = true;
		return this;
	}

	public BulletSpanDialogBuilder lightnessSliderOnly() {
		isLightnessSliderEnabled = true;
		isAlphaSliderEnabled = false;
		return this;
	}

	public BulletSpanDialogBuilder showAlphaSlider(boolean showAlpha) {
		isAlphaSliderEnabled = showAlpha;
		return this;
	}

	public BulletSpanDialogBuilder showLightnessSlider(boolean showLightness) {
		isLightnessSliderEnabled = showLightness;
		return this;
	}

	public BulletSpanDialogBuilder showBorder(boolean showBorder) {
		isBorderEnabled = showBorder;
		return this;
	}

	public BulletSpanDialogBuilder showColorEdit(boolean showEdit) {
		isColorEditEnabled = showEdit;
		return this;
	}

	public BulletSpanDialogBuilder setColorEditTextColor(int argb) {
		colorPickerView.setColorEditTextColor(argb);
		return this;
	}

	public BulletSpanDialogBuilder showColorPreview(boolean showPreview) {
		isPreviewEnabled = showPreview;
		if (!showPreview)
			pickerCount = 1;
		return this;
	}

	public BulletSpanDialogBuilder setPickerCount(int pickerCount) throws IndexOutOfBoundsException {
		if (pickerCount < 1 || pickerCount > 5)
			throw new IndexOutOfBoundsException("Picker Can Only Support 1-5 Colors");
		this.pickerCount = pickerCount;
		if (this.pickerCount > 1)
			this.isPreviewEnabled = true;
		return this;
	}

	public AlertDialog build() {
		Context context = builder.getContext();
		colorPickerView.setInitialColors(initialColor, getStartOffset(initialColor));
		colorPickerView.setShowBorder(isBorderEnabled);

		if (isLightnessSliderEnabled) {
			LinearLayout.LayoutParams layoutParamsForLightnessBar = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getDimensionAsPx(context, cc.brainbook.android.colorpicker.R.dimen.default_slider_height));
			lightnessSlider = new LightnessSlider(context);
			lightnessSlider.setLayoutParams(layoutParamsForLightnessBar);
			pickerContainer.addView(lightnessSlider);
			colorPickerView.setLightnessSlider(lightnessSlider);
			lightnessSlider.setColor(getStartColor(initialColor));
			lightnessSlider.setShowBorder(isBorderEnabled);
		}
		if (isAlphaSliderEnabled) {
			LinearLayout.LayoutParams layoutParamsForAlphaBar = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getDimensionAsPx(context, cc.brainbook.android.colorpicker.R.dimen.default_slider_height));
			alphaSlider = new AlphaSlider(context);
			alphaSlider.setLayoutParams(layoutParamsForAlphaBar);
			pickerContainer.addView(alphaSlider);
			colorPickerView.setAlphaSlider(alphaSlider);
			alphaSlider.setColor(getStartColor(initialColor));
			alphaSlider.setShowBorder(isBorderEnabled);
		}
		if (isColorEditEnabled) {
			LinearLayout.LayoutParams layoutParamsForColorEdit = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			colorEdit = (EditText) View.inflate(context, cc.brainbook.android.colorpicker.R.layout.color_edit, null);
			colorEdit.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
			colorEdit.setSingleLine();
			colorEdit.setVisibility(View.GONE);

			// limit number of characters to hexColors
			int maxLength = isAlphaSliderEnabled ? 9 : 7;
			colorEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});

			pickerContainer.addView(colorEdit, layoutParamsForColorEdit);

			colorEdit.setText(Utils.getHexString(getStartColor(initialColor), isAlphaSliderEnabled));
			colorPickerView.setColorEdit(colorEdit);
		}
		if (isPreviewEnabled) {
			colorPreview = (LinearLayout) View.inflate(context, cc.brainbook.android.colorpicker.R.layout.color_preview, null);
			colorPreview.setVisibility(View.GONE);
			pickerContainer.addView(colorPreview);

			if (initialColor.length == 0) {
				ImageView colorImage = (ImageView) View.inflate(context, cc.brainbook.android.colorpicker.R.layout.color_selector, null);
				colorImage.setImageDrawable(new ColorDrawable(Color.WHITE));
			} else {
				for (int i = 0; i < initialColor.length && i < this.pickerCount; i++) {
					if (initialColor[i] == null)
						break;
					LinearLayout colorLayout = (LinearLayout) View.inflate(context, cc.brainbook.android.colorpicker.R.layout.color_selector, null);
					ImageView colorImage = (ImageView) colorLayout.findViewById(cc.brainbook.android.colorpicker.R.id.image_preview);
					colorImage.setImageDrawable(new ColorDrawable(initialColor[i]));
					colorPreview.addView(colorLayout);
				}
			}
			colorPreview.setVisibility(View.VISIBLE);
			colorPickerView.setColorPreview(colorPreview, getStartOffset(initialColor));
		}

		mTextViewBulletRadius.setText(String.format(context.getResources().getString(R.string.bullet_span_bullet_radius), mBulletRadius));
		mSeekBarBulletRadius.setProgress(mBulletRadius);
		mTextViewGapWidth.setText(String.format(context.getResources().getString(R.string.bullet_span_gap_width), mGapWidth));
		mSeekBarGapWidth.setProgress(mGapWidth);

		return builder.create();
	}

	private Integer getStartOffset(Integer[] colors) {
		Integer start = 0;
		for (int i = 0; i < colors.length; i++) {
			if (colors[i] == null) {
				return start;
			}
			start = (i + 1) / 2;
		}
		return start;
	}

	private int getStartColor(Integer[] colors) {
		Integer startColor = getStartOffset(colors);
		return startColor == null ? Color.WHITE : colors[startColor];
	}

	private static int getDimensionAsPx(Context context, int rid) {
		return (int) (context.getResources().getDimension(rid) + .5f);
	}

	private void positiveButtonOnClick(DialogInterface dialog, ColorPickerClickListener onClickListener) {
		int selectedColor = colorPickerView.getSelectedColor();
		Integer[] allColors = colorPickerView.getAllColors();
		onClickListener.onClick(dialog, selectedColor, allColors);
	}

	public interface PickerClickListener {
		void onClick(DialogInterface d, int lastSelectedColor, Integer[] allColors, int bulletRadius, int gapWidth);
	}
}