package com.wello.wallpaperose.clock;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.wello.wallpaperose.AppConfig;
import com.wello.wallpaperose.R;
import com.wello.wallpaperose.advertise.AdNetworkHelper;
import com.wello.wallpaperose.utils.Tools;

import com.wello.wallpaperose.advertise.AdRewardedListener;

/**
 * Seçilen duvar kağıdının üzerinde saat modeli, renk, yazı tipi, yer, boyut ve gölge seçilir.
 * Önizlemedeki saat her saniye cihaz saatiyle yenilenir.
 */
public final class ClockStudio {

    private ClockStudio() {}

    public static void open(final AppCompatActivity activity, final String wallpaperId, final String imageUrl) {
        if (!AppConfig.CLOCK_ENABLE) return;
        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.activity_clock_studio);
        dialog.setCancelable(true);

        ClockFonts.bind(activity);
        ClockOverlayService.setShown(false);
        final ImageView photo = dialog.findViewById(R.id.clock_photo);
        final ClockOverlayView overlay = dialog.findViewById(R.id.clock_overlay);
        final TextView shadowLabel = dialog.findViewById(R.id.clock_shadow_label);
        final LinearLayout styles = dialog.findViewById(R.id.clock_styles);
        final LinearLayout colors = dialog.findViewById(R.id.clock_colors);
        final LinearLayout fonts = dialog.findViewById(R.id.clock_fonts);
        final View fontRow = dialog.findViewById(R.id.clock_font_row);
        final ClockPlacement placement = new ClockPlacement();
        placement.enabled = true;
        overlay.setPlacement(placement);

        Glide.with(activity).load(imageUrl).into(photo);
        final boolean clockAd = AppConfig.Ads.CLOCK_REWARDED
                && AppConfig.ads != null
                && AppConfig.ads.ad_enable;
        final AdNetworkHelper clockAds = clockAd ? new AdNetworkHelper(activity) : null;
        if (clockAds != null) {
            clockAds.loadRewardedAd(true, new AdRewardedListener() {
                @Override public void onComplete() {}
                @Override public void onDismissed() {}
                @Override public void onError() {}
            });
        }

        final ClockOverlayView.Chip[] chips = new ClockOverlayView.Chip[ClockPlacement.STYLE_COUNT];
        int chip = dp(activity, 68);
        int gap = dp(activity, 8);
        for (int i = 0; i < chips.length; i++) {
            final int style = i;
            ClockOverlayView.Chip view = new ClockOverlayView.Chip(activity);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(chip, chip);
            lp.setMarginEnd(gap);
            view.setLayoutParams(lp);
            view.setOnClickListener(v -> {
                placement.style = style;
                overlay.invalidate();
                bindChips(chips, placement);
                showFonts(fontRow, placement);
            });
            styles.addView(view);
            chips[i] = view;
        }

        final View[] swatches = new View[ClockPlacement.COLORS.length];
        int dot = dp(activity, 34);
        for (int i = 0; i < ClockPlacement.COLORS.length; i++) {
            final int color = ClockPlacement.COLORS[i];
            View view = new View(activity);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dot, dot);
            lp.setMarginEnd(gap);
            view.setLayoutParams(lp);
            view.setOnClickListener(v -> {
                placement.color = color;
                overlay.invalidate();
                bindColors(swatches, placement.color);
                bindChips(chips, placement);
            });
            colors.addView(view);
            swatches[i] = view;
        }

        final TextView[] fontViews = new TextView[ClockPlacement.FONT_COUNT];
        for (int i = 0; i < fontViews.length; i++) {
            final int font = i;
            TextView view = new TextView(activity);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(activity, 58), dp(activity, 40));
            lp.setMarginEnd(gap);
            view.setLayoutParams(lp);
            view.setGravity(android.view.Gravity.CENTER);
            view.setText("Aa");
            view.setTextSize(18);
            view.setTypeface(ClockPlacement.typeface(font));
            view.setOnClickListener(v -> {
                placement.font = font;
                overlay.invalidate();
                bindFonts(fontViews, placement.font);
                bindChips(chips, placement);
            });
            fonts.addView(view);
            fontViews[i] = view;
        }

        bindChips(chips, placement);
        bindColors(swatches, placement.color);
        bindFonts(fontViews, placement.font);
        showFonts(fontRow, placement);
        paintShadow(activity, shadowLabel, placement.shadow);

        dialog.findViewById(R.id.clock_back).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.clock_shadow_minus).setOnClickListener(v -> {
            placement.shadow = ClockStore.clampShadow(placement.shadow - 1);
            overlay.invalidate();
            paintShadow(activity, shadowLabel, placement.shadow);
        });
        dialog.findViewById(R.id.clock_shadow_plus).setOnClickListener(v -> {
            placement.shadow = ClockStore.clampShadow(placement.shadow + 1);
            overlay.invalidate();
            paintShadow(activity, shadowLabel, placement.shadow);
        });
        dialog.findViewById(R.id.clock_clear).setOnClickListener(v -> {
            ClockStore.releaseScreen(activity);
            Tools.showToastCenter(activity, R.string.clock_cleared);
            dialog.dismiss();
        });
        dialog.findViewById(R.id.clock_save).setOnClickListener(v -> {
            placement.enabled = true;
            v.setEnabled(false);
            Runnable install = () -> {
                boolean ok = ClockStore.installClockOnly(activity, placement);
                if (!ok) {
                    v.setEnabled(true);
                    Toast.makeText(activity,
                            "Saati göstermek için diğer uygulamaların üzerinde gösterme iznini açın, sonra Saati kur'a tekrar basın.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Tools.showToastCenter(activity, R.string.clock_saved);
                dialog.dismiss();
            };
            if (clockAds == null) {
                install.run();
                return;
            }
            boolean shown = clockAds.showRewardedAd(true, new AdRewardedListener() {
                @Override
                public void onComplete() {
                    install.run();
                }

                @Override
                public void onDismissed() {
                    v.setEnabled(true);
                }

                @Override
                public void onError() {
                    install.run();
                }
            });
            if (!shown) install.run();
        });
        dialog.setOnDismissListener(d -> {
            if (ClockStore.isClockOn(activity)) ClockOverlayService.setShown(true);
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setStatusBarColor(Color.BLACK);
        }
    }

    private static void bindChips(ClockOverlayView.Chip[] chips, ClockPlacement placement) {
        for (int i = 0; i < chips.length; i++) {
            chips[i].bind(i, placement.color, placement.font, i == placement.style);
        }
    }

    private static void bindColors(View[] swatches, int selected) {
        for (int i = 0; i < swatches.length; i++) {
            int color = ClockPlacement.COLORS[i];
            GradientDrawable oval = new GradientDrawable();
            oval.setShape(GradientDrawable.OVAL);
            oval.setColor(color);
            if (color == selected) {
                oval.setStroke(Math.max(3, swatches[i].getResources().getDisplayMetrics().densityDpi / 80), Color.WHITE);
            }
            swatches[i].setBackground(oval);
        }
    }

    private static void bindFonts(TextView[] views, int selected) {
        for (int i = 0; i < views.length; i++) {
            boolean on = i == selected;
            views[i].setBackgroundResource(on ? R.drawable.bg_clock_save : R.drawable.bg_clock_step);
            views[i].setTextColor(on ? Color.parseColor("#041018") : Color.WHITE);
        }
    }

    private static void showFonts(View fontRow, ClockPlacement placement) {
        fontRow.setVisibility(placement.digital() ? View.VISIBLE : View.GONE);
    }

    private static void paintShadow(AppCompatActivity activity, TextView label, int shadow) {
        label.setText(activity.getString(R.string.clock_shadow, shadow));
    }

    private static int dp(AppCompatActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
