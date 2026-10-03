package com.wello.wallpaperose;

import com.wello.wallpaperose.utils.AppConfigExt;
import com.wello.wallpaperose.utils.BillingHelper.FeatureUseType;

import java.io.Serializable;

public class AppConfig extends AppConfigExt implements Serializable {

    /* -------------------------------------- INSTRUCTION : ----------------------------------------
     * This is config file used for this app, you can configure Ads, Notification, and General data from this file
     * some values are not explained and can be understood easily according to the variable name
     * value can change remotely (optional), please read documentation to follow instruction
     *
     * variable with UPPERCASE name will NOT fetch / replace with remote config
     * variable with LOWERCASE name will fetch / replace with remote config
     * See video Remote Config tutorial https://www.youtube.com/watch?v=tOKXwOTqOzA
     ----------------------------------------------------------------------------------------------*/

    /* set true for fetch config with firebase remote config, */
    public static final boolean USE_REMOTE_CONFIG = true;

    /* force rtl layout direction */
    public static final boolean RTL_LAYOUT = false;

    /* İlk kurulum tanıtımı. true = ilk açılışta gösterilir, false = hiç açılmaz. */
    public static final boolean SHOW_ONBOARDING = true;

    /* Saat. true = saat menüsü ve ekrandaki saat açık.
     * false = saat menüsü gizlenir, kurulu saat kapanır.
     * Ana ekran, kilit, her ikisi ve crop bu anahtardan etkilenmez.
     * Açmak için yalnızca bu satırı true yap.
     */
    public static final boolean CLOCK_ENABLE = false;

    /* config for General Application */
    public static class General {

        /* Edit Blogger with yours. Make sure its not using http and doesn't have backslash('/') in the end url. */
        /* ex https://resep-app.blogspot.com/ to resep-app.blogspot.com */
        public String blogger_url = "wallpaper-areapp.blogspot.com";

        /* access key generated from : https://dream-space.web.id/blogger/access-key  */
        public String access_key = "QjbwT4oKsUzSYo4pOr/7G39iEGVGG59Il7YNEd6JZ/ZNWWYvSBkLl7rMEuYhWj//:ZmVkY2JhOTg3NjU0MzIxMA==";

        /* prefix name for image file saved on device */
        public String prefix_filename = "waller_";
        public String download_directory = "Waller";

        /* true for sort category alphabetically */
        public boolean sort_category_alphabetically = true;

        /* fill this values when you publish app not in google play */
        public String non_playstore_market_android = "https://appgallery.huawei.com/#/app/C107483149";

        /* true for open link in internal app browser, not external app browser */
        public boolean open_link_in_app = true;

        /* amount data each api request listing, used on menu Home and Activity Category Details */
        public Integer listing_pagination_count = 20;
        /* 3 links below will use on setting page */
        public String privacy_policy_url = "https://privacypolicy808.blogspot.com/2024/04/privacy-policy-this-privacy-policy-is.html";
        public String more_apps_url = "https://appgallery.huawei.com/#/app/C107483149";
        public String contact_us_url = "https://appgallery.huawei.com/#/app/C107483149";
    }

    /* config for Ad Network */
    public static class Ads {

        /* Yalnızca AdMob. false yapılırsa AdMob de açılmaz.
         * final değil: final olursa değer diğer sınıfların içine gömülür.
         * Değiştirdikten sonra Clean Project, sonra Rebuild yap.
         */
        public static boolean USE_ADMOB = true;

        /* Saat kurma ödüllü reklamı. Yalnızca "Saati kur" için geçerlidir.
         * true  = saat kurulmadan önce ödüllü reklam izlenir.
         * false = saat reklamsız kurulur.
         * İndirme, kırpma, ana ekran, kilit ekranı ve her ikisi bu anahtardan etkilenmez.
         */
        public static final boolean CLOCK_REWARDED = true;

        /* enable disable ads */
        public boolean ad_enable = true;

        /* ad backup flow retry attempt cycle */
        public Integer retry_from_start_max = 3;

        public boolean ad_enable_gdpr = true;

        /* disable enable ads each page */
        public boolean ad_main_banner = true;
        public boolean ad_main_interstitial = true;
        public boolean ad_listing_details_banner = true;
        public boolean ad_news_details_banner = true;
        public boolean ad_category_details_banner = true;
        public boolean ad_search_banner = true;
        public boolean ad_splash_open_app = true;
        public boolean ad_global_open_app = false;

        /* when ad networks not supported open app format, it will replace with interstitial format
         * for placement after plash screen only */
        public boolean ad_replace_unsupported_open_app_with_interstitial_on_splash = false;

        /* maximum load time in second for open app ads */
        public Integer limit_time_open_app_loading = 10;

        /* show interstitial after several action, this value for action counter */
        public Integer ad_inters_interval = 5;

        /* ad unit for ADMOB */
        public String ad_admob_publisher_id = "pub-4522936465566239";
        public String ad_admob_banner_unit_id = "ca-app-pub-4522936465566239/6756902117";
        public String ad_admob_interstitial_unit_id = "ca-app-pub-4522936465566239/2267208367";
        public String ad_admob_rewarded_unit_id = "ca-app-pub-4522936465566239/5378350535";
        public String ad_admob_open_app_unit_id = "ca-app-pub-4522936465566239/5447905479";

        /* ad unit for Google Ad Manager */
        public String ad_manager_banner_unit_id = "/xxxx/example/banner";
        public String ad_manager_interstitial_unit_id = "/xxxx/example/interstitial";
        public String ad_manager_rewarded_unit_id = "/xxxx/example/rewarded";
        public String ad_manager_open_app_unit_id = "/xxxx/example/app-open";

        /* ad unit for FAN */
        public String ad_fan_banner_unit_id = "YOUR_PLACEMENT_ID";
        public String ad_fan_interstitial_unit_id = "YOUR_PLACEMENT_ID";
        public String ad_fan_rewarded_unit_id = "YOUR_PLACEMENT_ID";

        /* ad unit for IRON SOURCE */
        public String ad_ironsource_app_key = "xxxxxxxxx";
        public String ad_ironsource_banner_unit_id = "DefaultBanner";
        public String ad_ironsource_rewarded_unit_id = "DefaultRewardedVideo";
        public String ad_ironsource_interstitial_unit_id = "DefaultInterstitial";

        /* ad unit for UNITY */
        public String ad_unity_game_id = "5615671";
        public String ad_unity_banner_unit_id = "Banner_Android";
        public String ad_unity_rewarded_unit_id = "Rewarded_Android";
        public String ad_unity_interstitial_unit_id = "Interstitial_Android";

        /* ad unit for APPLOVIN MAX */
        public String ad_applovin_banner_unit_id = "a3a3a5b44c76xxxx";
        public String ad_applovin_interstitial_unit_id = "a3a3a5b44c76xxxx";
        public String ad_applovin_rewarded_unit_id = "a3a3a5b44c76xxxx";
        public String ad_applovin_open_app_unit_id = "a3a3a5b44c76xxxx";

        /* ad unit for APPLOVIN DISCOVERY */
        public String ad_applovin_banner_zone_id = "df40a31072fexxxx";
        public String ad_applovin_interstitial_zone_id = "d0eea040d4bdxxxx";
        public String ad_applovin_rewarded_zone_id = "5d799aeefef7xxxx";

        /* ad unit for STARTAPP */
        public String ad_startapp_app_id = "0";

        /* ad unit for WORTISE */
        public String ad_wortise_app_id = "test-app-id";
        public String ad_wortise_banner_unit_id = "test-bannerx";
        public String ad_wortise_interstitial_unit_id = "test-interstitialx";
        public String ad_wortise_rewarded_unit_id = "test-rewardedx";
        public String ad_wortise_open_app_unit_id = "test-app-openx";
    }

    /* One Signal Notification */
    public static class Notification {
        public String notif_one_signal_appid = "7664a0aa-aa55-47a2-af0e-561645f657d8";
    }

    /* config for Google Play Billing */
    public static class Billing {

        /* enable disable billing */
        public boolean billing_enable = false;

        /* product id from google play console Monetize > Products > Subscriptions */
        public String billing_product_id = "subscription_product_id";

        /* disable enable ads each page */
        public boolean billing_weekly_enable = false;
        public boolean billing_monthly_enable = false;
        public boolean billing_yearly_enable = false;

        /* limitation feature usage, before user force to purchase */
        public int billing_feature_use_limit = 5;

        /* limitation feature usage type, DAILY, WEEKLY, MONTHLY, default = DAILY */
        public FeatureUseType billing_feature_use_type = FeatureUseType.DAILY;
    }
}