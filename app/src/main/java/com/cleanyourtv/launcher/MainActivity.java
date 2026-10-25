package com.cleanyourtv.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Light, ad-free home screen for Fire TV.
 * No external libraries, plain views, D-pad friendly.
 */
public class MainActivity extends Activity {

    private static final String PREFS = "launcher";
    private static final String KEY_LAST = "last_pkg";

    // Apps we never show on our home screen (shopping, ads, demos, tutorials).
    private static final Set<String> HIDDEN = new HashSet<>(Arrays.asList(
            "com.amazon.tv.launcher",
            "com.amazon.shoptv.client",
            "com.amazon.shoptv.firetv.client",
            "com.amazon.alexashopping",
            "com.amazon.minitv.android.app",
            "com.amazon.imdb.tv.android.app",
            "com.amazon.gamehub",
            "com.amazon.bueller.music",
            "com.amazon.bueller.photos",
            "com.amazon.naatyam",
            "com.amazon.kor.demo",
            "com.amazon.storm.lightning.tutorial",
            "com.amazon.tmm.tutorial",
            "com.amazon.tv.livetv"
    ));

    // These come first, in this order.
    private static final List<String> PINNED = Arrays.asList(
            "com.netflix.ninja",
            "com.amazon.firetv.youtube",
            "com.amazon.firebat"
    );

    private static final String[] AMAZON_SETTINGS_PKGS = {
            "com.amazon.tv.settings.v2",
            "com.amazon.tv.settings.core",
            "com.amazon.tv.settings"
    };

    private static class AppEntry {
        String pkg;
        String cls;
        CharSequence label;
        Drawable image;
        boolean banner;
    }

    private static class SettingItem {
        final String label;
        final String action;

        SettingItem(String label, String action) {
            this.label = label;
            this.action = action;
        }
    }

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    @Override
    public void onBackPressed() {
        // Home screen: Back does nothing.
    }

    // ---------------------------------------------------------------- data

    private List<AppEntry> loadApps() {
        PackageManager pm = getPackageManager();
        Map<String, AppEntry> map = new LinkedHashMap<>();
        addFrom(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, map);
        addFrom(pm, Intent.CATEGORY_LAUNCHER, map);
        List<AppEntry> list = new ArrayList<>(map.values());
        Collections.sort(list, (a, b) -> {
            int ia = PINNED.indexOf(a.pkg);
            int ib = PINNED.indexOf(b.pkg);
            if (ia < 0) ia = Integer.MAX_VALUE;
            if (ib < 0) ib = Integer.MAX_VALUE;
            if (ia != ib) return ia < ib ? -1 : 1;
            return a.label.toString().compareToIgnoreCase(b.label.toString());
        });
        return list;
    }

    private void addFrom(PackageManager pm, String category, Map<String, AppEntry> map) {
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(category);
        List<ResolveInfo> found = pm.queryIntentActivities(q, 0);
        for (ResolveInfo ri : found) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || HIDDEN.contains(pkg) || map.containsKey(pkg)) {
                continue;
            }
            AppEntry e = new AppEntry();
            e.pkg = pkg;
            e.cls = ri.activityInfo.name;
            e.label = ri.loadLabel(pm);
            Drawable b = ri.activityInfo.loadBanner(pm);
            if (b != null) {
                e.image = b;
                e.banner = true;
            } else {
                e.image = ri.loadIcon(pm);
            }
            map.put(pkg, e);
        }
    }

    private List<SettingItem> settingItems() {
        List<SettingItem> l = new ArrayList<>();
        l.add(new SettingItem("Wi-Fi", Settings.ACTION_WIFI_SETTINGS));
        l.add(new SettingItem("Display", Settings.ACTION_DISPLAY_SETTINGS));
        l.add(new SettingItem("Sound", Settings.ACTION_SOUND_SETTINGS));
        l.add(new SettingItem("Developer", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        l.add(new SettingItem("All settings", Settings.ACTION_SETTINGS));
        return l;
    }

    // ------------------------------------------------------------- actions

    private void launchApp(AppEntry e) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setClassName(e.pkg, e.cls);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(i);
            prefs.edit().putString(KEY_LAST, e.pkg).apply();
        } catch (Exception ex) {
            Toast.makeText(this, "Could not open " + e.label, Toast.LENGTH_SHORT).show();
        }
    }

    private void openSetting(SettingItem s) {
        try {
            startActivity(new Intent(s.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        } catch (Exception ignored) {
            // Fire OS may not handle this action, fall back below.
        }
        PackageManager pm = getPackageManager();
        for (String p : AMAZON_SETTINGS_PKGS) {
            Intent l = pm.getLaunchIntentForPackage(p);
            if (l != null) {
                try {
                    l.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(l);
                    return;
                } catch (Exception ignored) {
                    // try next
                }
            }
        }
        Toast.makeText(this, "Settings not available", Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------ UI

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void build() {
        List<AppEntry> apps = loadApps();

        FrameLayout root = new FrameLayout(this);
        root.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0B1220, 0xFF1B2540}));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipChildren(false);
        sv.setClipToPadding(false);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);
        col.setClipToPadding(false);
        col.setPadding(dp(48), dp(28), dp(48), dp(28));
        sv.addView(col, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Header: title and clock
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Home", 30, Color.WHITE, true);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm a");
        clock.setFormat24Hour("h:mm a");
        clock.setTextSize(24);
        clock.setTextColor(0xFFCBD5F0);
        header.addView(clock);
        col.addView(header);

        View firstFocus = null;

        // Featured banner: last opened app, else first app
        AppEntry featured = null;
        String last = prefs.getString(KEY_LAST, null);
        for (AppEntry e : apps) {
            if (e.pkg.equals(last)) {
                featured = e;
                break;
            }
        }
        if (featured == null && !apps.isEmpty()) featured = apps.get(0);

        if (featured != null) {
            final AppEntry f = featured;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(20), dp(16), dp(20), dp(16));
            ImageView img = new ImageView(this);
            img.setImageDrawable(f.image);
            img.setScaleType(f.banner ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.CENTER_INSIDE);
            card.addView(img, new LinearLayout.LayoutParams(dp(260), dp(146)));
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(24), 0, 0, 0);
            info.addView(text(last != null && last.equals(f.pkg) ? "CONTINUE" : "START", 14, 0xFF8FA3D6, true));
            info.addView(text(f.label.toString(), 34, Color.WHITE, true));
            card.addView(info);
            makeFocusable(card, () -> launchApp(f), dp(16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(20);
            col.addView(card, lp);
            firstFocus = card;
        }

        // Apps row
        col.addView(sectionTitle("Apps"));
        LinearLayout appRow = new LinearLayout(this);
        appRow.setOrientation(LinearLayout.HORIZONTAL);
        appRow.setClipChildren(false);
        for (AppEntry e : apps) {
            final AppEntry a = e;
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setPadding(dp(8), dp(8), dp(8), dp(8));
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(a.image);
            if (a.banner) {
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                iv.setPadding(dp(24), dp(12), dp(24), dp(12));
                iv.setBackgroundColor(0xFF0F172E);
            }
            tile.addView(iv, new LinearLayout.LayoutParams(dp(184), dp(104)));
            TextView label = text(a.label.toString(), 15, 0xFFE6ECFF, false);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            label.setPadding(0, dp(8), 0, 0);
            tile.addView(label, new LinearLayout.LayoutParams(dp(184), ViewGroup.LayoutParams.WRAP_CONTENT));
            makeFocusable(tile, () -> launchApp(a), dp(12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(14);
            appRow.addView(tile, lp);
            if (firstFocus == null) firstFocus = tile;
        }
        col.addView(row(appRow));

        // Settings row
        col.addView(sectionTitle("Settings"));
        LinearLayout setRow = new LinearLayout(this);
        setRow.setOrientation(LinearLayout.HORIZONTAL);
        setRow.setClipChildren(false);
        for (SettingItem s : settingItems()) {
            final SettingItem si = s;
            TextView t = text(si.label, 18, Color.WHITE, true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(12), dp(12), dp(12), dp(12));
            makeFocusable(t, () -> openSetting(si), dp(12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(160), dp(80));
            lp.rightMargin = dp(14);
            setRow.addView(t, lp);
            if (firstFocus == null) firstFocus = t;
        }
        col.addView(row(setRow));

        root.addView(sv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        if (firstFocus != null) firstFocus.requestFocus();
    }

    private HorizontalScrollView row(LinearLayout inner) {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipChildren(false);
        hs.setClipToPadding(false);
        hs.setPadding(0, dp(12), 0, dp(12));
        hs.addView(inner);
        return hs;
    }

    private TextView sectionTitle(String s) {
        TextView t = text(s, 18, 0xFF9AA7C7, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(26);
        t.setLayoutParams(lp);
        return t;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private void makeFocusable(View v, Runnable action, int radiusPx) {
        v.setFocusable(true);
        v.setClickable(true);
        v.setOnClickListener(x -> action.run());

        GradientDrawable normal = new GradientDrawable();
        normal.setColor(0xFF1A2340);
        normal.setCornerRadius(radiusPx);
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(0xFF2B3A6B);
        focused.setCornerRadius(radiusPx);
        focused.setStroke(dp(3), Color.WHITE);
        StateListDrawable sl = new StateListDrawable();
        sl.addState(new int[]{android.R.attr.state_focused}, focused);
        sl.addState(new int[]{}, normal);
        v.setBackground(sl);

        v.setOnFocusChangeListener((view, has) ->
                view.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(120).start());
    }
}
