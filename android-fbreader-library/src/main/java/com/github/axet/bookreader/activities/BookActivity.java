package com.github.axet.bookreader.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.github.axet.androidlibrary.activities.AppCompatFullscreenThemeActivity;
import com.github.axet.androidlibrary.app.FileTypeDetector;
import com.github.axet.androidlibrary.preferences.AboutPreferenceCompat;
import com.github.axet.androidlibrary.preferences.RotatePreferenceCompat;
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter;
import com.github.axet.androidlibrary.widgets.ErrorDialog;
import com.github.axet.androidlibrary.widgets.OpenChoicer;
import com.github.axet.androidlibrary.widgets.OpenFileDialog;
import com.github.axet.androidlibrary.widgets.SearchView;
import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.androidlibrary.widgets.WebViewCustom;

import com.github.axet.bookreader.app.BookApplication;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.fragments.FragmentBookMark;
import com.github.axet.bookreader.fragments.LibraryFragment;
import com.github.axet.bookreader.fragments.ReaderFragment;
import com.github.axet.bookreader.util.Util;
import com.github.axet.bookreader.viewmodel.MainViewModel;
import com.github.axet.bookreader.widgets.FBReaderView;

import com.google.android.material.internal.NavigationMenuItemView;
import com.google.android.material.navigation.NavigationView;
import org.geometerplus.fbreader.fbreader.options.ImageOptions;
import org.geometerplus.fbreader.fbreader.options.MiscOptions;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.geometerplus.zlibrary.ui.android.R;

import static com.github.axet.bookreader.widgets.FBReaderView.ACTION_MENU;

public class BookActivity extends AppCompatFullscreenThemeActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = BookActivity.class.getSimpleName();

    public static final int RESULT_FILE = 1;
    public static final int RESULT_ADD_CATALOG = 2;
    public static final String PATH_BOOK = "PATH_BOOK";

    public Toolbar toolbar;
    protected ConstraintLayout layoutMenu;
    protected ImageView btnTTS, btnBookMark, btnFont, btnMucLuc, btnSearch, btnCloseSearch,
            btnOption;
    protected SearchView actionSearch;
   // private FullscreenActivity.ListenAction mListenAction;
    protected ImageView btnBack;
    protected Group mGroup;
    protected MainViewModel mMainViewModel;
    private ListenAction mListenAction;
    Storage storage;
    boolean isRunning;
    OpenChoicer choicer;
    String lastSearch;

    LibraryFragment libraryFragment = LibraryFragment.newInstance();
    public boolean volumeEnabled = true; // tmp enabled / disable volume keys

    public void setListenAction(ListenAction listenAction) {
        mListenAction = listenAction;
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_MENU)) toggle();
        }
    };

    public static Intent newInstance(Context context, String pathBook) {
        Intent intent = new Intent(context, BookActivity.class);
        intent.putExtra(PATH_BOOK, pathBook);
        return intent;
    }

    public interface SearchListener {
        String getHint();

        void search(String s);

        void searchClose();
    }

    public interface ListenAction {
        void actionFullScreen();

        void actionMucLuc();

        void actionFont(ImageView imageView);

        void actionBookMark();

        void extendView(ImageView imageView);

        void actionTTS();
    }

    public interface OnBackPressed {
        boolean onBackPressed();
    }

    public static class SortByPage implements Comparator<Storage.RecentInfo> {
        @Override
        public int compare(Storage.RecentInfo o1, Storage.RecentInfo o2) {
            int r = Integer.valueOf(o1.position.getParagraphIndex())
                    .compareTo(o2.position.getParagraphIndex());
            if (r != 0) return r;
            return Integer.valueOf(o1.position.getElementIndex())
                    .compareTo(o2.position.getElementIndex());
        }
    }

    @SuppressLint("RestrictedApi")
    public static View findView(ViewGroup p, MenuItem item) {
        for (int i = 0; i < p.getChildCount(); i++) {
            View v = p.getChildAt(i);
            if (v instanceof ViewGroup) {
                View m = findView((ViewGroup) v, item);
                if (m != null) return m;
            }
            if (v instanceof NavigationMenuItemView) {
                if (((NavigationMenuItemView) v).getItemData() == item) return v;
            }
            if (v.getId() == item.getItemId()) return v;
        }
        return null;
    }

    public static class ResourcesMap extends HashMap<String, String> {
        public ResourcesMap(Context context, int k, int v) {
            String[] kk = context.getResources().getStringArray(k);
            String[] vv = context.getResources().getStringArray(v);
            for (int i = 0; i < kk.length; i++)
                put(kk[i], vv[i]);
        }
    }

    public static class ProgressDialog extends AlertDialog.Builder {
        public Handler handler = new Handler();
        public ProgressBar load;
        public ProgressBar v;
        public TextView text;
        public AlertDialog dialog;
        public Storage.Progress progress = new Storage.Progress() {
            @Override
            public void progress(final long bytes, final long total) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ProgressDialog.this.progress(bytes, total);
                    }
                });
            }
        };

        public ProgressDialog(Context context) {
            super(context);
            int dp10 = ThemeUtils.dp2px(context, 10);

            final LinearLayout ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.VERTICAL);
            v = new ProgressBar(context);
            v.setIndeterminate(true);
            v.setPadding(dp10, dp10, dp10, dp10);
            ll.addView(v);
            load = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
            load.setPadding(dp10, dp10, dp10, dp10);
            load.setMax(100);
            ll.addView(load);
            text = new TextView(context);
            text.setPadding(dp10, dp10, dp10, dp10);
            ll.addView(text);
            load.setVisibility(View.GONE);
            text.setVisibility(View.GONE);

            setTitle(R.string.loading_book);
            setView(ll);
            setCancelable(false);
            setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
        }

        public void progress(long bytes, long total) {
            String str = BookApplication.formatSize(getContext(), bytes);
            if (total > 0) {
                str += " / " + BookApplication.formatSize(getContext(), total);
                load.setProgress((int) (bytes * 100 / total));
                load.setVisibility(View.VISIBLE);
                v.setVisibility(View.GONE);
            } else {
                load.setVisibility(View.GONE);
                v.setVisibility(View.VISIBLE);
            }
            str += String.format(" (%s%s)",
                    BookApplication.formatSize(getContext(), progress.info.getCurrentSpeed()),
                    getContext().getString(R.string.per_second));
            text.setText(str);
            text.setVisibility(View.VISIBLE);
        }

        public void build() {
            dialog = super.create();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_main);
        mMainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        layoutMenu = (ConstraintLayout) findViewById(R.id.layoutMenu);
        btnBookMark = (ImageView) findViewById(R.id.imgBookMark);
        btnMucLuc = (ImageView) findViewById(R.id.imgMucLuc);
        btnFont = (ImageView) findViewById(R.id.imgFont);
        btnBack = findViewById(R.id.imgBack);
        mGroup = findViewById(R.id.group);
        btnTTS = (ImageView) findViewById(R.id.imgTTS);
        btnSearch = (ImageView) findViewById(R.id.iconSearch);
        actionSearch = (SearchView) findViewById(R.id.imgSearch);
        btnCloseSearch = (ImageView) findViewById(R.id.iconClose);
        btnOption = (ImageView) findViewById(R.id.imageOption);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(getResources().getColor(R.color.white)));
        }
        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setTint(Color.BLACK);
        }
        Log.d("ducNQ", "onCreatesavedInstanceState: ");
        storage = new Storage(this);

        registerReceiver(receiver, new IntentFilter(ACTION_MENU));

        if (savedInstanceState == null
                && getIntent().getParcelableExtra(SAVE_INSTANCE_STATE) == null) {
            openLibrary();
            openIntent(getIntent());
        }


        RotatePreferenceCompat.onCreate(this, BookApplication.PREFERENCE_ROTATE);

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        shared.registerOnSharedPreferenceChangeListener(this);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMainViewModel.eventOpenNote.setValue(false);
                onBackPressed();
            }
        });
        btnOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String last = shared.getString(BookApplication.PREFERENCE_LAST_PATH, null);
                Uri old = null;
                if (last != null) {
                    old = Uri.parse(last);
                    File f = Storage.getFile(old);
                    while (f != null && !f.exists()) f = f.getParentFile();
                    if (f != null) old = Uri.fromFile(f);
                } else {
                    old = Uri.parse(
                            ContentResolver.SCHEME_CONTENT + Storage.CSS); // show SAF default
                }
                choicer = new OpenChoicer(OpenFileDialog.DIALOG_TYPE.FILE_DIALOG, true) {
                    @Override
                    public void onResult(Uri uri) {
                        String s = uri.getScheme();
                        if (s.equals(ContentResolver.SCHEME_FILE)) {
                            File f = Storage.getFile(uri);
                            f = f.getParentFile();
                            SharedPreferences.Editor editor = shared.edit();
                            editor.putString(BookApplication.PREFERENCE_LAST_PATH, f.toString());
                            editor.commit();
                        }
                        loadBook(uri, null);
                    }
                };
                choicer.setStorageAccessFramework(BookActivity.this, RESULT_FILE);
                choicer.setPermissionsDialog(BookActivity.this, Storage.PERMISSIONS_RO,
                        RESULT_FILE);
                choicer.show(old);
            }
        });
        setOnClick();
        observerData();
        String pathBook = getIntent().getStringExtra(PATH_BOOK);
        Uri uri = Uri.parse(pathBook);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadBook(uri, null);
            }
        },100);

    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        List<Fragment> ff = fm.getFragments();
        mMainViewModel.eventOpenNote.setValue(false);
        Fragment fragmentBookMark =
                getSupportFragmentManager().findFragmentByTag(FragmentBookMark.TAG);
        if (fragmentBookMark != null && fragmentBookMark.isVisible()) {
            super.onBackPressed();
            return;
        }
        if (ff != null && !ff.isEmpty()) {
            for (Fragment f : ff) {
                if (f != null && f.isVisible() && f instanceof OnBackPressed) {
                    OnBackPressed s = (OnBackPressed) f;
                    if (s.onBackPressed()) return;
                }
            }
        }
        super.onBackPressed();
        if (fm.getBackStackEntryCount() == 0) onResume(); // udpate theme if changed
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem searchMenu = menu.findItem(R.id.action_search);
        //        MenuItem fullScreen = menu.findItem(R.id.actionFullScreen);
        MenuItem actionToc = menu.findItem(R.id.action_toc);
        MenuItem actionFont = menu.findItem(R.id.action_fontsize);

        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        MenuItem theme = menu.findItem(R.id.action_theme);

        //setColorFilterForIcon
        searchMenu.getIcon().setTint(Color.BLACK);
        theme.getIcon().setTint(Color.BLACK);
        //  fullScreen.getIcon().setTint(Color.BLACK);
        actionToc.getIcon().setTint(Color.BLACK);

        String t = shared.getString(BookApplication.PREFERENCE_THEME, "");
        if (t.equals(getString(R.string.Theme_System))) {
            theme.setVisible(false);
        } else {
            theme.setVisible(true);
            String d = getString(R.string.Theme_Dark);
            theme.setIcon(t.equals(d) ? R.drawable.ic_brightness_night_white_24dp
                    : R.drawable.ic_brightness_day_white_24dp);
            ResourcesMap map = new ResourcesMap(this, R.array.themes_values, R.array.themes_text);
            theme.setTitle(
                    map.get(getString(t.equals(d) ? R.string.Theme_Dark : R.string.Theme_Light)));
        }

        // final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenu);
        //actionSearch.setBackgroundColor(Color.GRAY);
        actionSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public boolean onQueryTextSubmit(String query) {
                lastSearch = query;
                actionSearch.clearFocus();
                FragmentManager fm = getSupportFragmentManager();
                for (Fragment f : fm.getFragments()) {
                    if (f != null && f.isVisible() && f instanceof SearchListener) {
                        SearchListener s = (SearchListener) f;
                        s.search(actionSearch.getQuery().toString());
                    }
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        actionSearch.setOnSearchClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                if (lastSearch != null && !lastSearch.isEmpty()) {
                    actionSearch.setQuery(lastSearch, false);
                }
                FragmentManager fm = getSupportFragmentManager();
                for (Fragment f : fm.getFragments()) {
                    if (f != null && f.isVisible() && f instanceof SearchListener) {
                        SearchListener s = (SearchListener) f;
                        actionSearch.setQueryHint(s.getHint());
                    }
                }
            }
        });
        actionSearch.setOnCollapsedListener(new SearchView.OnCollapsedListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onCollapsed() {
                FragmentManager fm = getSupportFragmentManager();
                for (Fragment f : fm.getFragments()) {
                    if (f != null && f.isVisible() && f instanceof SearchListener) {
                        SearchListener s = (SearchListener) f;
                        s.searchClose();
                    }
                }
            }
        });
        actionSearch.setOnCloseButtonListener(new SearchView.OnCloseButtonListener() {
            @Override
            public void onClosed() {
                lastSearch = "";
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about) {
            AboutPreferenceCompat.buildDialog(this, R.raw.about).show();
            return true;
        }

        /*if (id == R.id.actionFullScreen) {
          //  sendBroadcast(new Intent(ACTION_MENU));
            return true;
        }*/

        if (id == R.id.action_settings) {
            SettingsActivity.startActivity(this);
            return true;
        }

        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        if (id == R.id.action_file) {
            String last = shared.getString(BookApplication.PREFERENCE_LAST_PATH, null);
            Uri old = null;
            if (last != null) {
                old = Uri.parse(last);
                File f = Storage.getFile(old);
                while (f != null && !f.exists()) f = f.getParentFile();
                if (f != null) old = Uri.fromFile(f);
            } else {
                old = Uri.parse(ContentResolver.SCHEME_CONTENT + Storage.CSS); // show SAF default
            }
            choicer = new OpenChoicer(OpenFileDialog.DIALOG_TYPE.FILE_DIALOG, true) {
                @Override
                public void onResult(Uri uri) {
                    String s = uri.getScheme();
                    if (s.equals(ContentResolver.SCHEME_FILE)) {
                        File f = Storage.getFile(uri);
                        f = f.getParentFile();
                        SharedPreferences.Editor editor = shared.edit();
                        editor.putString(BookApplication.PREFERENCE_LAST_PATH, f.toString());
                        editor.commit();
                    }
                    loadBook(uri, null);
                }
            };
            choicer.setStorageAccessFramework(this, RESULT_FILE);
            choicer.setPermissionsDialog(this, Storage.PERMISSIONS_RO, RESULT_FILE);
            choicer.show(old);
        }

        if (id == R.id.action_theme) {
            SharedPreferences.Editor edit = shared.edit();
            String t = shared.getString(BookApplication.PREFERENCE_THEME, "");
            String d = getString(R.string.Theme_Dark);
            edit.putString(BookApplication.PREFERENCE_THEME,
                    t.equals(d) ? getString(R.string.Theme_Light) : d);
            edit.commit();
            restartActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_library) openLibrary();

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openIntent(intent);
    }

    void openIntent(Intent intent) {
        if (intent == null) return;
        String a = intent.getAction();
        if (a == null) return;
        Uri u = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (u == null) u = intent.getData();
        if (u == null) {
            String t = intent.getStringExtra(Intent.EXTRA_TEXT); // handling SEND intents
            if (t != null && t.startsWith(WebViewCustom.SCHEME_HTTP)) u = Uri.parse(t);
        }
        if (u == null) return;
        loadBook(u, null);
    }

    public void loadBook(final Uri u, final Runnable success) {
        final ProgressDialog builder = new ProgressDialog(this);
        final AlertDialog d = builder.create();
        d.show();
        Thread thread = new Thread("load book") {
            @Override
            public void run() {
                final Thread t = Thread.currentThread();
                d.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                t.interrupt();
                            }
                        });
                try {
                    final Storage.Book book = storage.load(u, builder.progress);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing() || !isRunning) return;
                            loadBook(book);
                            if (success != null) success.run();
                        }
                    });
                } catch (FileTypeDetector.DownloadInterrupted e) {
                    Log.d(TAG, "interrupted", e);
                } catch (Throwable e) {
                    ErrorDialog.Post(BookActivity.this, e);
                } finally {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            d.cancel();
                        }
                    });
                }
            }
        };
        thread.start();
    }

    public void showHideToolbar(boolean check) {
        if (layoutMenu != null) {
            layoutMenu.setVisibility(check ? View.GONE : View.VISIBLE);
        }
    }

    @SuppressLint("RestrictedApi")
    public void loadBook(final Storage.Book book) {
        final List<Uri> uu = storage.recentUris(book);
        if (uu.size() > 1) {
            LayoutInflater inflater = LayoutInflater.from(this);

            AlertDialog.Builder builder = new AlertDialog.Builder((Context) this);

            final ArrayList<ZLTextPosition> selected = new ArrayList<>();

            selected.clear();
            selected.add(book.info.position);

            final Storage.FBook fbook = storage.read(book);

            final Runnable done = new Runnable() {
                @Override
                public void run() {
                    fbook.close();

                    for (Uri u : uu) {
                        try {
                            Storage.RecentInfo info = new Storage.RecentInfo(BookActivity.this, u);
                            book.info.merge(info);
                        } catch (Exception e) {
                            Log.d(TAG, "unable to merge info", e);
                        }
                        Storage.delete(BookActivity.this, u);
                    }
                    book.info.position = selected.get(0);
                    storage.save(book);

                    openBook(book.url);
                }
            };

            builder.setTitle(R.string.sync_conflict);

            View v = inflater.inflate(R.layout.recent, null);

            final FBReaderView r = (FBReaderView) v.findViewById(R.id.recent_fbview);

            // r.config.setValue(r.app.ViewOptions.ScrollbarType, 0);
            r.config.setValue(r.app.MiscOptions.WordTappingAction,
                    MiscOptions.WordTappingActionEnum.doNothing);
            r.config.setValue(r.app.ImageOptions.TapAction, ImageOptions.TapActionEnum.doNothing);

            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
            String mode = shared.getString(BookApplication.PREFERENCE_VIEW_MODE, "");
            r.setWidget(mode.equals(FBReaderView.Widgets.CONTINUOUS.toString())
                    ? FBReaderView.Widgets.CONTINUOUS : FBReaderView.Widgets.PAGING);

            r.loadBook(fbook);

            LinearLayout pages = (LinearLayout) v.findViewById(R.id.recent_pages);

            List<Storage.RecentInfo> rr = new ArrayList<>();

            for (Uri u : uu) {
                try {
                    Storage.RecentInfo info = new Storage.RecentInfo(BookActivity.this, u);
                    if (info.position != null) {
                        boolean found = false;
                        for (int i = 0; i < rr.size(); i++) {
                            Storage.RecentInfo ii = rr.get(i);
                            if (ii.position.getParagraphIndex() == info.position.getParagraphIndex()
                                    && ii.position.getElementIndex()
                                    == info.position.getElementIndex()) {
                                found = true;
                            }
                        }
                        if (!found) {
                            rr.add(info);
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Unable to read info", e);
                }
            }

            Collections.sort(rr, new SortByPage());

            if (rr.size() == 1) {
                done.run();
                return;
            }

            for (int i = 0; i < rr.size(); i++) {
                final Storage.RecentInfo info = rr.get(i);
                TextView p = (TextView) inflater.inflate(R.layout.recent_item, pages, false);
                if (info.position != null) {
                    p.setText(info.position.getParagraphIndex()
                            + "."
                            + info.position.getElementIndex());
                }
                p.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        r.gotoPosition(info.position);
                        selected.clear();
                        selected.add(info.position);
                    }
                });
                pages.addView(p);
            }

            builder.setView(v);

            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    fbook.close();
                }
            });

            builder.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    done.run();
                }
            });

            builder.show();
            return;
        }
        openBook(book.url);
    }

    @SuppressLint("RestrictedApi")
    public void popBackStack(String tag, int flags) { // only pop existing TAG
        FragmentManager fm = getSupportFragmentManager();
        if (tag == null) {
            fm.popBackStack(null, flags);
            return;
        }
        for (int i = 0; i < fm.getBackStackEntryCount(); i++) {
            String n = fm.getBackStackEntryAt(i).getName();
            if (n != null && n.equals(tag)) {
                fm.popBackStack(tag, flags);
                return;
            }
        }
    }

    @SuppressLint("RestrictedApi")
    public Fragment getCurrentFragment() {
        FragmentManager fm = getSupportFragmentManager();
        List<Fragment> ff = fm.getFragments();
        if (ff == null) return null;
        for (Fragment f : ff) {
            if (f != null && f.isVisible()) return f;
        }
        return null;
    }

    public void openBook(Uri uri) {
        popBackStack(ReaderFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        addFragment(ReaderFragment.newInstance(uri), ReaderFragment.TAG).commit();
    }

    public void openBook(Uri uri, FBReaderView.ZLTextIndexPosition pos) {
        popBackStack(ReaderFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        addFragment(ReaderFragment.newInstance(uri, pos), ReaderFragment.TAG).commit();
    }

    public void openLibrary() {
      /*  FragmentManager fm = getSupportFragmentManager();
        popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        openFragment(libraryFragment, LibraryFragment.TAG).commit();
        onResume(); // update theme if changed*/
    }

    public FragmentTransaction addFragment(Fragment f, String tag) {
        return openFragment(f, tag).addToBackStack(tag);
    }

    public FragmentTransaction openFragment(Fragment f, String tag) {
        FragmentManager fm = getSupportFragmentManager();
        return fm.beginTransaction().replace(R.id.main_content, f, tag);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        RotatePreferenceCompat.onDestroy(this);
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        shared.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
        Util.closeKeyboard(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RESULT_FILE:
            case RESULT_ADD_CATALOG:
                if (choicer != null) // called twice or activity reacated
                {
                    choicer.onRequestPermissionsResult(permissions, grantResults);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESULT_FILE:
            case RESULT_ADD_CATALOG:
                if (choicer != null) // called twice or activity reacated
                {
                    choicer.onActivityResult(resultCode, data);
                }
                break;
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        if (volumeEnabled && shared.getBoolean(BookApplication.PREFERENCE_VOLUME_KEYS, false)) {
            FragmentManager fm = getSupportFragmentManager();
            for (Fragment f : fm.getFragments()) {
                if (f != null && f.isVisible() && f instanceof ReaderFragment) {
                    if (((ReaderFragment) f).onKeyDown(keyCode, event)) return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        if (volumeEnabled && shared.getBoolean(BookApplication.PREFERENCE_VOLUME_KEYS, false)) {
            FragmentManager fm = getSupportFragmentManager();
            for (Fragment f : fm.getFragments()) {
                if (f != null && f.isVisible() && f instanceof ReaderFragment) {
                    if (((ReaderFragment) f).onKeyUp(keyCode, event)) return true;
                }
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isRunning = true;
        RotatePreferenceCompat.onResume(this, BookApplication.PREFERENCE_ROTATE);
        CacheImagesAdapter.cacheClear(this);
        BookApplication.from(this).ttf.preloadFonts();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(BookApplication.PREFERENCE_THEME)) invalidateOptionsMenu();
    }
    @SuppressLint({ "InlinedApi", "RestrictedApi" })
    @Override
    public void setFullscreen(boolean b) {
        super.setFullscreen(b);
        FragmentManager fm = getSupportFragmentManager();
        List<Fragment> ff = fm.getFragments();
        if (ff != null) {
            for (Fragment f : ff) {
                if (f instanceof FullscreenActivity.FullscreenListener) {
                    ((FullscreenActivity.FullscreenListener) f).onFullscreenChanged(b);
                }
            }
        }
    }

    @Override
    public void hideSystemUI() {
        super.hideSystemUI();
        setFitsSystemWindows(this, false);
    }

    @Override
    public void showSystemUI() {
        super.showSystemUI();
        setFitsSystemWindows(this, true);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        FragmentManager fm = getSupportFragmentManager();
        List<Fragment> ff = fm.getFragments();
        if (ff != null) {
            for (Fragment f : ff) {
                if (f instanceof FullscreenActivity.FullscreenListener) ((FullscreenActivity.FullscreenListener) f).onUserInteraction();
            }
        }
    }
    @Override
    public int getAppTheme() {
        return BookApplication.getTheme(this, R.style.AppThemeLight_NoActionBar,
                R.style.AppThemeDark_NoActionBar);
    }

    @Override
    public int getAppThemePopup() {
        return BookApplication.getTheme(this, R.style.AppThemeLight_PopupOverlay,
                R.style.AppThemeDark_PopupOverlay);
    }
    private void observerData() {
        //        mMainViewModel.eventShowBookMark.observe(this, aBoolean -> {
        //            if (btnBookMark != null) {
        //                btnBookMark.setVisibility(aBoolean ? View.VISIBLE : View.GONE);
        //            }
        //        });
        mMainViewModel.eventFont.observe((LifecycleOwner) this, aBoolean -> {
            if (btnFont != null) {
                // btnFont.setVisibility(aBoolean ? View.VISIBLE : View.GONE);
            }
        });
        mMainViewModel.eventSearchN.observe((LifecycleOwner) this, aBoolean -> {
            if (btnSearch != null) {
                btnSearch.setVisibility(aBoolean ? View.VISIBLE : View.GONE);
            }
        });
        mMainViewModel.eventAddBookMark.observe((LifecycleOwner) this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (btnBookMark != null) {
                    btnBookMark.setImageResource(
                            aBoolean ? R.drawable.ic_bookmark_white_24dp : R.drawable.ic_bookmark);
                }
            }
        });
        mMainViewModel.eventOpenNote.observe((LifecycleOwner) this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    mGroup.setVisibility(View.VISIBLE);
                    layoutMenu.setVisibility(View.INVISIBLE);
                } else {
                    mGroup.setVisibility(View.INVISIBLE);
                    layoutMenu.setVisibility(View.VISIBLE);
                }
            }
        });
       /* mMainViewModel.eventShowMucLuc.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (btnMucLuc != null) btnMucLuc.setVisibility(aBoolean ? View.VISIBLE : View.GONE);
            }
        });*/
    }
    private void setOnClick() {
        btnMucLuc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListenAction == null) return;
                mListenAction.actionMucLuc();
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionSearch.setVisibility(View.VISIBLE);
                actionSearch.onActionViewExpanded();
                btnSearch.setVisibility(View.GONE);
                btnCloseSearch.setVisibility(View.VISIBLE);
                btnMucLuc.setVisibility(View.INVISIBLE);
            }
        });
        btnCloseSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionSearch.setVisibility(View.INVISIBLE);
                actionSearch.onActionViewCollapsed();
                btnSearch.setVisibility(View.VISIBLE);
                btnCloseSearch.setVisibility(View.INVISIBLE);
                btnMucLuc.setVisibility(View.VISIBLE);
            }
        });
        btnFont.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListenAction == null) return;
                mListenAction.actionFont(btnFont);
            }
        });
        btnBookMark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListenAction == null) return;
                mListenAction.actionBookMark();
            }
        });
       /* btnTTS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListenAction == null) return;
                mListenAction.actionTTS();
            }
        });*/
        if (mListenAction != null) {
            mListenAction.extendView(btnBookMark);
        }
    }
}
