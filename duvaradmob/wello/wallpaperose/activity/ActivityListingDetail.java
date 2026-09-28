package com.wello.wallpaperose.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.wello.wallpaperose.AppConfig;
import com.wello.wallpaperose.R;
import com.wello.wallpaperose.advertise.AdCounters;
import com.wello.wallpaperose.advertise.AdNetworkHelper;
import com.wello.wallpaperose.data.ThisApp;
import com.wello.wallpaperose.databinding.ActivityListingDetailBinding;
import com.wello.wallpaperose.model.Wallpaper;
import com.wello.wallpaperose.model.type.ApplyType;
import com.wello.wallpaperose.room.table.EntityListing;
import com.wello.wallpaperose.clock.ClockStore;
import com.wello.wallpaperose.clock.ClockStudio;
import com.wello.wallpaperose.utils.BillingHelper;
import com.wello.wallpaperose.utils.Downloader;
import com.wello.wallpaperose.utils.FavoriteImageStore;
import com.wello.wallpaperose.utils.TitleTranslations;
import com.wello.wallpaperose.utils.NotificationLoading;
import com.wello.wallpaperose.utils.PermissionUtil;
import com.wello.wallpaperose.utils.Tools;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.wello.wallpaperose.advertise.AdRewardedListener;

public class ActivityListingDetail extends AppCompatActivity {

    private static final String EXTRA_OBJECT   = "key.EXTRA_OBJECT";
    private static final String EXTRA_POSITION = "key.EXTRA_POSITION";

    private Wallpaper wallpaper;
    private List<Wallpaper> wallpapers = new ArrayList<>();
    private int position = -1;

    private Downloader downloader;
    private ActivityListingDetailBinding binding;
    private AdNetworkHelper adNetworkHelper;

    // Ödüllü reklam için bekleyen işlem
    private Runnable pendingAction = null;

    private FloatingActionButton fabMenu, fabShare, fabFavorite, fabDownload;
    private FloatingActionButton fabSetHome, fabSetLock, fabSetBoth, fabCrop, fabClock;
    private LinearLayout lytShare, lytFavorite, lytDownload;
    private LinearLayout lytSetHome, lytSetLock, lytSetBoth, lytCrop, lytClock;
    private View scrim;
    private boolean isFabOpen = false;

    // Menü elemanlarının FAB'a en yakından en uzağa doğru sırası (kaskad animasyon için)
    private LinearLayout[] subMenuBottomToTop() {
        if (!AppConfig.CLOCK_ENABLE) {
            return new LinearLayout[]{lytShare, lytFavorite, lytDownload, lytCrop, lytSetBoth, lytSetLock, lytSetHome};
        }
        return new LinearLayout[]{lytShare, lytFavorite, lytDownload, lytCrop, lytClock, lytSetBoth, lytSetLock, lytSetHome};
    }

    // ── Navigation ───────────────────────────────────────────────────────

    public static void navigate(Activity activity, Wallpaper wallpaper) {
        ThisApp.itemsWallpaper = new ArrayList<>();
        Intent i = new Intent(activity, ActivityListingDetail.class);
        i.putExtra(EXTRA_OBJECT, wallpaper);
        activity.startActivity(i);
    }

    public static void navigate(Activity activity, int position) {
        Intent i = new Intent(activity, ActivityListingDetail.class);
        i.putExtra(EXTRA_POSITION, position);
        activity.startActivity(i);
    }

    public static void navigate(AppCompatActivity activity, ArrayList<Wallpaper> list, int position) {
        Intent intent = new Intent(activity, ActivityListingDetail.class);
        intent.putExtra("EXTRA_LIST", list);
        intent.putExtra("EXTRA_POS", position);
        activity.startActivity(intent);
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListingDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        position = getIntent().getIntExtra(EXTRA_POSITION, -1);
        if (position == -1) {
            wallpaper = (Wallpaper) getIntent().getSerializableExtra(EXTRA_OBJECT);
            wallpapers.add(wallpaper);
        } else {
            wallpapers = ThisApp.itemsWallpaper;
            wallpaper  = wallpapers.get(position);
        }

        downloader = new Downloader(this);
        initComponent();
        initToolbar();
        displayWallpaperData();
        setupAds();
        Tools.RTLMode(getWindow());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adNetworkHelper != null) adNetworkHelper.destroyAndDetachBanner();
    }

    // ── UI ───────────────────────────────────────────────────────────────

    private void initComponent() {
        if (position == -1) {
            binding.counter.setVisibility(View.GONE);
        } else {
            binding.counter.setText((position + 1) + "/" + wallpapers.size());
        }
        binding.btInfo.setOnClickListener(v -> showDialogInfo());

        fabMenu     = findViewById(R.id.fab_menu);
        fabShare    = findViewById(R.id.fab_share);
        fabFavorite = findViewById(R.id.fab_favorite);
        fabDownload = findViewById(R.id.fab_download);
        fabSetHome  = findViewById(R.id.fab_set_home);
        fabSetLock  = findViewById(R.id.fab_set_lock);
        fabSetBoth  = findViewById(R.id.fab_set_both);
        fabCrop     = findViewById(R.id.fab_crop);
        fabClock    = findViewById(R.id.fab_clock);

        lytShare    = findViewById(R.id.lyt_action_share);
        lytFavorite = findViewById(R.id.lyt_action_favorite);
        lytDownload = findViewById(R.id.lyt_action_download);
        lytSetHome  = findViewById(R.id.lyt_action_set_home);
        lytSetLock  = findViewById(R.id.lyt_action_set_lock);
        lytSetBoth  = findViewById(R.id.lyt_action_set_both);
        lytCrop     = findViewById(R.id.lyt_action_crop);
        lytClock    = findViewById(R.id.lyt_action_clock);
        if (!AppConfig.CLOCK_ENABLE && lytClock != null) lytClock.setVisibility(View.GONE);

        // YENİ: menü açıkken ekranın herhangi bir yerine (karartılmış alana) dokununca menüyü kapat
        scrim = findViewById(R.id.scrim);
        scrim.setOnClickListener(v -> {
            if (isFabOpen) toggleFabMenu();
        });

        fabMenu.setOnClickListener(v -> toggleFabMenu());
    }

    private void initToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        binding.toolbar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        setSupportActionBar(binding.toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeButtonEnabled(true);
            ab.setTitle(null);
        }
    }

    // ── Reklam Kurulumu ──────────────────────────────────────────────────

    private void setupAds() {
        adNetworkHelper = new AdNetworkHelper(this);
        adNetworkHelper.updateConsentStatus();

        // Alt banner
        adNetworkHelper.loadBannerAd(AppConfig.ads.ad_listing_details_banner);

        // Ödüllü reklamı arka planda önceden yükle
        adNetworkHelper.loadRewardedAd(true, new AdRewardedListener() {
            @Override public void onComplete()  { }
            @Override public void onDismissed() { }
            @Override public void onError()     { }
        });
    }

    /**
     * Ödüllü reklam göster → tamamlanınca action çalıştır.
     *
     * onComplete()  → kullanıcı reklamı izledi, ödül verildi → action çalışır
     * onDismissed() → kullanıcı reklamı kapattı → action çalışmaz
     * onError()     → reklam yüklenemedi → action direkt çalışır
     */
    private void showRewardedThenRun(final Runnable action) {
        pendingAction = action;
        boolean shown = adNetworkHelper.showRewardedAd(true, new AdRewardedListener() {
            @Override
            public void onComplete() {
                // Kullanıcı reklamı izledi → işlemi çalıştır
                if (pendingAction != null) {
                    pendingAction.run();
                    pendingAction = null;
                }
            }

            @Override
            public void onDismissed() {
                // Kullanıcı reklamı kapattı → işlem çalışmaz
                pendingAction = null;
            }

            @Override
            public void onError() {
                // Reklam yüklenemedi → işlemi direkt çalıştır
                if (pendingAction != null) {
                    pendingAction.run();
                    pendingAction = null;
                }
            }
        });

        if (!shown) {
            // Reklam gösterilemedi → direkt çalıştır
            if (pendingAction != null) {
                pendingAction.run();
                pendingAction = null;
            }
        }
    }

    // ── FAB Tıklama ──────────────────────────────────────────────────────

    public void detailsActionClick(View v) {
        int id = v.getId();
        if (id == R.id.fab_download) {
            showRewardedThenRun(this::downloadAction);
        } else if (id == R.id.fab_set_home) {
            showRewardedThenRun(() -> directSetWallpaper(ApplyType.HOME));
        } else if (id == R.id.fab_set_lock) {
            showRewardedThenRun(() -> directSetWallpaper(ApplyType.LOCK));
        } else if (id == R.id.fab_set_both) {
            showRewardedThenRun(() -> directSetWallpaper(ApplyType.BOTH));
        } else if (id == R.id.fab_clock) {
            if (!AppConfig.CLOCK_ENABLE) return;
            if (isFabOpen) closeFabMenu();
            ClockStudio.open(this, wallpaper.id, wallpaper.image);
        } else if (id == R.id.fab_crop) {
            // YENİ: Kırpma ekranını aç — diğer "duvar kağıdı ayarla" aksiyonlarıyla
            // aynı ödüllü reklam kapısından geçer, kırpma ekranının kendisi
            // ayrıca reklam istemez.
            showRewardedThenRun(() -> ActivityCropWallpaper.navigate(this, wallpaper.image, wallpaper.id));
        } else if (id == R.id.fab_favorite) {
            Tools.vibrate(this, 25); // YENİ: favori dokunuşunda kısa titreşim
            if (!is_favorite) {
                // YENİ: favoriye eklenme anını ve beğeni/indirme sayaçlarını kaydet
                EntityListing existing = ThisApp.dao().getListing(wallpaper.id);
                EntityListing entity = EntityListing.entity(wallpaper);
                entity.setSaved_date(System.currentTimeMillis());
                entity.setLikeCount(existing != null ? existing.getLikeCount() + 1 : 1);
                entity.setDownloadCount(existing != null ? existing.getDownloadCount() : 0);
                ThisApp.dao().insertListing(entity);
            } else {
                ThisApp.dao().deleteListing(wallpaper.id);
                FavoriteImageStore.delete(this, wallpaper.id);
            }
            refreshFavorite();
            AdCounters.onFavoriteTap(this);
        } else if (id == R.id.fab_share) {
            shareAction();
        }
    }

    public boolean checkBillingIfPremium() {
        if (wallpaper.premium && !BillingHelper.checkPurchased()) {
            showDialogPremium();
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    // YENİ: Sistem geri tuşuna basıldığında + menüsü açıksa önce onu kapat,
    // ekrandan direkt çıkma sadece menü kapalıyken gerçekleşsin.
    @Override
    public void onBackPressed() {
        if (isFabOpen) {
            toggleFabMenu();
        } else {
            super.onBackPressed();
        }
    }

    // ── FAB Animasyon ────────────────────────────────────────────────────

    private static final long STAGGER_STEP_MS = 45L;

    private void toggleFabMenu() {
        if (isFabOpen) {
            closeFabMenu();
        } else {
            openFabMenu();
        }
    }

    private void openFabMenu() {
        isFabOpen = true;

        fabMenu.animate().rotation(45f).setDuration(320)
                .setInterpolator(new OvershootInterpolator(2.2f)).start();

        showScrim(true);

        LinearLayout[] items = subMenuBottomToTop();
        for (int i = 0; i < items.length; i++) {
            openSubMenu(items[i], i * STAGGER_STEP_MS);
        }
    }

    private void closeFabMenu() {
        isFabOpen = false;

        fabMenu.animate().rotation(0f).setDuration(220)
                .setInterpolator(new DecelerateInterpolator()).start();

        showScrim(false);

        LinearLayout[] items = subMenuBottomToTop();
        // Kapanışta en uzaktaki (ANA EKRAN) önce, FAB'a en yakın (PAYLAŞ) en son kapanır —
        // açılışın tersi yönde akan doğal bir "toparlanma" hissi verir.
        for (int i = 0; i < items.length; i++) {
            closeSubMenu(items[i], (items.length - 1 - i) * STAGGER_STEP_MS);
        }
    }

    private void openSubMenu(View view, long delay) {
        if (view == null) return;
        view.animate().cancel();
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setScaleX(0.6f);
        view.setScaleY(0.6f);
        view.setTranslationY(40f);
        view.animate()
                .translationY(0f).alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(delay)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(1.3f))
                .start();
    }

    private void closeSubMenu(View view, long delay) {
        if (view == null) return;
        view.animate().cancel();
        view.animate()
                .translationY(30f).alpha(0f).scaleX(0.7f).scaleY(0.7f)
                .setStartDelay(delay)
                .setDuration(180)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> view.setVisibility(View.INVISIBLE))
                .start();
    }

    private void showScrim(final boolean show) {
        if (scrim == null) return;
        scrim.animate().cancel();
        if (show) {
            scrim.setAlpha(0f);
            scrim.setVisibility(View.VISIBLE);
            scrim.animate().alpha(1f).setDuration(250).start();
        } else {
            scrim.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> scrim.setVisibility(View.GONE)).start();
        }
    }

    // ── Duvar Kağıdı ─────────────────────────────────────────────────────

    private void directSetWallpaper(final ApplyType type) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> loadBitmapAndSetWallpaper(type), 200);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void loadBitmapAndSetWallpaper(final ApplyType type) {
        Glide.with(this).asBitmap().load(wallpaper.image).into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                try {
                    if (type == ApplyType.HOME || type == ApplyType.BOTH) {
                        ClockStore.publish(ActivityListingDetail.this, bitmap, WallpaperManager.FLAG_SYSTEM);
                    }
                    if (type == ApplyType.LOCK || type == ApplyType.BOTH) {
                        ClockStore.publish(ActivityListingDetail.this, bitmap, WallpaperManager.FLAG_LOCK);
                    }
                    FavoriteImageStore.save(ActivityListingDetail.this, wallpaper.id, bitmap);
                    Tools.showToastCenter(ActivityListingDetail.this, R.string.set_success);
                    Tools.vibrate(ActivityListingDetail.this, 40); // YENİ: duvar kağıdı ayarlanınca titreşim
                } catch (IOException e) {
                    e.printStackTrace();
                    Tools.showToastCenter(ActivityListingDetail.this, R.string.set_failed);
                }
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                super.onLoadFailed(errorDrawable);
                Tools.showToastCenter(ActivityListingDetail.this, R.string.set_failed);
            }
        });
    }

    // ── İndir ────────────────────────────────────────────────────────────

    @TargetApi(Build.VERSION_CODES.M)
    private void downloadAction() {
        if (!PermissionUtil.isStorageGranted(this)) {
            if (ThisApp.pref().getNeverAskAgain(PermissionUtil.STORAGE)) {
                PermissionUtil.showDialog(this);
            } else {
                requestPermissions(PermissionUtil.PERMISSION_STORAGE, 500);
            }
            return;
        }
        if (Downloader.isFileExist(getApplicationContext(), wallpaper.getFilename())) {
            Tools.showToastCenter(this, R.string.already_download);
            return;
        }
        final NotificationLoading nl = new NotificationLoading(this, Integer.parseInt(wallpaper.getFilename()));
        nl.start(getString(R.string.wallpaper_download));
        downloader.setDownloadListener(result -> {
            nl.stop(getString(result.success ? R.string.download_complete : R.string.download_failed), result);
            // YENİ: "En Çok İndirilenler" sekmesi için indirme sayacını artır
            if (result.success) {
                ThisApp.dao().incrementDownloadCount(wallpaper.id);
                Tools.vibrate(this, 40); // YENİ: indirme tamamlanınca titreşim
            }
        });
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                downloader.startDownload(getApplicationContext(), wallpaper), 200);
    }

    // ── Paylaş ───────────────────────────────────────────────────────────

    @TargetApi(Build.VERSION_CODES.M)
    private void shareAction() {
        if (!PermissionUtil.isStorageGranted(this)) {
            requestPermissions(PermissionUtil.PERMISSION_STORAGE, 500);
            if (ThisApp.pref().getNeverAskAgain(PermissionUtil.STORAGE)) {
                Tools.showToastCenter(this, R.string.storage_permission_denied);
            }
            return;
        }
        downloader.startDownload(wallpaper, result -> {
            if (!result.success) {
                Tools.showToastCenter(this, R.string.failed_prepare_share);
                return;
            }
            Tools.methodShare(this, Downloader.getFile(getApplicationContext(), wallpaper.getFilename()));
        });
    }

    // ── İzin ─────────────────────────────────────────────────────────────

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 500) {
            for (String perm : permissions) {
                boolean rationale = shouldShowRequestPermissionRationale(perm);
                ThisApp.pref().setNeverAskAgain(perm, !rationale);
            }
            if (PermissionUtil.isStorageGranted(this)) downloadAction();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // ── Favori ───────────────────────────────────────────────────────────

    private boolean is_favorite = false;

    private void refreshFavorite() {
        if (fabFavorite == null) return;
        if (ThisApp.dao().getListing(wallpaper.id) != null) {
            fabFavorite.setImageResource(R.drawable.ic_favorite_added);
            is_favorite = true;
        } else {
            fabFavorite.setImageResource(R.drawable.ic_favorite);
            is_favorite = false;
        }
    }

    // ── Wallpaper Görüntüleme ────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    private void displayWallpaperData() {
        binding.title.setText(TitleTranslations.display(wallpaper.title));
        ViewPager mViewPager = findViewById(R.id.view_pager);
        int peek = Math.round(8 * getResources().getDisplayMetrics().density);
        int gap = Math.round(8 * getResources().getDisplayMetrics().density);
        mViewPager.setPadding(peek, 0, peek, 0);
        mViewPager.setClipToPadding(false);
        mViewPager.setPageMargin(gap);
        mViewPager.setPageTransformer(false, (page, pos) -> {
            float abs = Math.min(Math.abs(pos), 1f);
            float scale = 1f - (0.1f * abs);
            page.setScaleY(scale);
            page.setScaleX(scale);
        }, View.LAYER_TYPE_NONE);
        mViewPager.setAdapter(new ViewPagerAdapter(this, wallpapers));
        if (position != -1) mViewPager.setCurrentItem(position);
        else binding.counter.setVisibility(View.GONE);
        Tools.applyFrost(this, findViewById(R.id.frost_image), findViewById(R.id.frost_color), wallpaper.image);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override public void onPageScrolled(int p, float o, int px) { }
            @Override public void onPageSelected(int pos) {
                wallpaper = wallpapers.get(pos);
                binding.title.setText(TitleTranslations.display(wallpaper.title));
                Tools.applyFrost(ActivityListingDetail.this, findViewById(R.id.frost_image), findViewById(R.id.frost_color), wallpaper.image);
                refreshFavorite();
                binding.counter.setText((pos + 1) + "/" + wallpapers.size());
                binding.icPremium.setVisibility(wallpaper.premium ? View.VISIBLE : View.GONE);
            }
            @Override public void onPageScrollStateChanged(int state) { }
        });

        refreshFavorite();
        binding.icPremium.setVisibility(wallpaper.premium ? View.VISIBLE : View.GONE);
    }

    // ── Dialoglar ────────────────────────────────────────────────────────

    private void showDialogInfo() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_more_info);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(true);

        final FlexboxLayout categoryBox = dialog.findViewById(R.id.category_flex_box);
        if (wallpaper.category == null || wallpaper.category.isEmpty()) {
            categoryBox.setVisibility(View.GONE);
            dialog.show();
            return;
        }
        categoryBox.removeAllViews();
        for (String cat : wallpaper.category) {
            TextView tv = new TextView(this);
            int mH = getResources().getDimensionPixelOffset(R.dimen.spacing_4);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(mH, mH + 5, mH, mH);
            tv.setLayoutParams(params);
            int pH = getResources().getDimensionPixelOffset(R.dimen.spacing_15);
            tv.setPadding(pH, tv.getPaddingTop(), pH, tv.getPaddingTop());
            tv.setGravity(Gravity.CENTER);
            tv.setClickable(true);
            tv.setFocusable(true);
            tv.setBackgroundResource(R.drawable.button_category);
            tv.setText(cat);
            tv.setTextColor(getResources().getColor(R.color.textIconPrimary));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.txt_small));
            tv.setOnClickListener(view -> ActivityCategoryDetail.navigate(this, cat));
            categoryBox.addView(tv);
        }
        String type = wallpaper.premium ? getString(R.string.PREMIUM) : getString(R.string.FREE);
        ((TextView) dialog.findViewById(R.id.type)).setText(type);
        dialog.show();
    }

    private void showDialogPremium() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_premium);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(true);
        dialog.findViewById(R.id.bt_upgrade).setOnClickListener(v -> { dialog.dismiss(); ActivityBilling.navigate(this); });
        dialog.findViewById(R.id.bt_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ── ViewPager Adapter ────────────────────────────────────────────────

    class ViewPagerAdapter extends PagerAdapter {
        Context context;
        List<Wallpaper> wallpapers;
        LayoutInflater mLayoutInflater;

        ViewPagerAdapter(Context context, List<Wallpaper> wallpapers) {
            this.context = context;
            this.wallpapers = wallpapers;
            mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override public int getCount() { return wallpapers.size(); }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == ((LinearLayout) object);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View itemView = mLayoutInflater.inflate(R.layout.item_image_wallpaper_details, container, false);
            ImageView main_image = itemView.findViewById(R.id.main_image);
            Tools.displayImageWallpaperDetails(context, main_image, wallpapers.get(position).image);
            Objects.requireNonNull(container).addView(itemView);
            return itemView;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((LinearLayout) object);
        }
    }
}