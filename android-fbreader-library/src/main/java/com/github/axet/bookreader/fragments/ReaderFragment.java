package com.github.axet.bookreader.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.github.axet.androidlibrary.preferences.ScreenlockPreference;
import com.github.axet.androidlibrary.widgets.ErrorDialog;
import com.github.axet.androidlibrary.widgets.PopupWindowCompat;
import com.github.axet.androidlibrary.widgets.SearchView;
import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.androidlibrary.widgets.TreeListView;
import com.github.axet.androidlibrary.widgets.TreeRecyclerView;
import com.github.axet.bookreader.activities.FullscreenActivity;
import com.github.axet.bookreader.activities.BookActivity;
import com.github.axet.bookreader.app.BookApplication;
import com.github.axet.bookreader.app.ComicsPlugin;
import com.github.axet.bookreader.app.Plugin;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.custom.NavigationSeekbar;
import com.github.axet.bookreader.viewmodel.MainViewModel;
import com.github.axet.bookreader.widgets.BookmarksDialog;
import com.github.axet.bookreader.widgets.FBReaderView;
import com.github.axet.bookreader.widgets.FontsPopup;
import com.github.axet.bookreader.widgets.ScrollWidget;
import com.github.axet.bookreader.widgets.ToolbarButtonView;
import com.github.axet.bookreader.fragments.DialogBookFragment;
import java.util.List;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.R;

import static com.github.axet.bookreader.app.Storage.TYPE_BOOKMARK;

public class ReaderFragment extends Fragment
        implements BookActivity.SearchListener, SharedPreferences.OnSharedPreferenceChangeListener,
        FullscreenActivity.FullscreenListener, BookActivity.OnBackPressed {
    public static final String TAG = ReaderFragment.class.getSimpleName();
    // private KeyboardHeightProvider keyboardHeightProvider;
    public static final int FONT_START = 15;
    public static final int FONT_END = 100;
    public static final int REFLOW_START = 3;
    public static final int REFLOW_END = 15;

    public static final int RESULT_FONTS = 1;

    Handler handler = new Handler();
    Storage storage;
    Storage.Book book;
    Storage.FBook fbook;
    FBReaderView fb;
    AlertDialog tocdialog;
    boolean showRTL;
    View mViewKeyboard;
    NavigationSeekbar mNavigationSeekbar;
    FontsPopup fontsPopup;
    MenuItem searchMenu;
    ConstraintLayout mView;
    BroadcastReceiver battery;
    private DialogBookFragment mDialogBookFragment;
    ImageView btnOption, btnBookMark, btnFullScreen, btnMucLuc;
    SearchView btnSearch;
    MainViewModel mMainViewModel;
    Runnable invalidateOptionsMenu;
    Runnable time = new Runnable() {
        @Override
        public void run() {
            long s60 = 60 * 1000;
            long secs = System.currentTimeMillis() % s60;
            handler.removeCallbacks(this);
            long d = s60 - secs;
            if (d < 1000) d = s60 + d;
            handler.postDelayed(this, d);
            fb.invalidateFooter();
            savePosition();
        }
    };

    public static View getOverflowMenuButton(Activity a) {
        return getOverflowMenuButton((ViewGroup) a.findViewById(R.id.toolbar));
    }

    public static View getOverflowMenuButton(ViewGroup p) {
        for (int i = 0; i < p.getChildCount(); i++) {
            View v = p.getChildAt(i);
            if (v.getClass().getCanonicalName().contains("OverflowMenuButton")) return v;
            if (v instanceof ViewGroup) {
                v = getOverflowMenuButton((ViewGroup) v);
                if (v != null) return v;
            }
        }
        return null;
    }

    public static class TOCHolder extends TreeRecyclerView.TreeHolder {
        ImageView i;
        TextView textView;

        public TOCHolder(View itemView) {
            super(itemView);
            i = (ImageView) itemView.findViewById(R.id.image);
            textView = (TextView) itemView.findViewById(R.id.text);
        }
    }

    public class TOCAdapter extends TreeRecyclerView.TreeAdapter<TOCHolder> {
        TOCTree current;

        public TOCAdapter(List<TOCTree> ll, TOCTree current) {
            this.current = current;
            loadTOC(root, ll);
            load();
        }

        void loadTOC(TreeListView.TreeNode r, List<TOCTree> tree) {
            for (TOCTree t : tree) {
                TreeListView.TreeNode n = new TreeListView.TreeNode(r, t);
                r.nodes.add(n);
                if (equals(t, current)) {
                    n.selected = true; // current selected
                    r.expanded = true; // parent expanded
                }
                if (t.hasChildren()) {
                    loadTOC(n, t.subtrees());
                    if (n.expanded) {
                        n.selected = true;
                        r.expanded = true;
                    }
                }
            }
        }

        public int getCurrent() {
            for (int i = 0; i < getItemCount(); i++) {
                TOCTree t = (TOCTree) getItem(i).tag;
                if (equals(t, current)) return i;
            }
            return -1;
        }

        @Override
        public TOCHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View convertView = inflater.inflate(R.layout.toc_item, null);
            return new TOCHolder(convertView);
        }

        @Override
        public void onBindViewHolder(final TOCHolder h, int position) {
            TreeListView.TreeNode t = getItem(h.getAdapterPosition(this));
            TOCTree tt = (TOCTree) t.tag;
            ImageView ex = (ImageView) h.itemView.findViewById(R.id.expand);
            if (t.nodes.isEmpty()) {
                ex.setVisibility(View.INVISIBLE);
            } else {
                ex.setVisibility(View.VISIBLE);
            }
            ex.setImageResource(t.expanded ? R.drawable.ic_expand_less_black_24dp
                    : R.drawable.ic_expand_more_black_24dp);
            h.itemView.setPadding(20 * t.level, 0, 0, 0);
            if (t.selected) {
                h.textView.setTypeface(null, Typeface.BOLD);
                h.i.setColorFilter(null);
            } else {
                h.i.setColorFilter(Color.GRAY);
                h.textView.setTypeface(null, Typeface.NORMAL);
            }
            h.textView.setText(tt.getText());
            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TOCTree n = (TOCTree) getItem(h.getAdapterPosition(TOCAdapter.this)).tag;
                    if (n.hasChildren()) return;
                    fb.gotoPosition(n.getReference());
                    if (mDialogBookFragment != null && mDialogBookFragment.isVisible()) {
                        mDialogBookFragment.dismissAllowingStateLoss();
                    }
                    // tocdialog.dismiss();
                }
            });
        }

        boolean equals(TOCTree t, TOCTree t2) {
            if (t == null || t2 == null) return false;
            TOCTree.Reference r1 = t.getReference();
            TOCTree.Reference r2 = t2.getReference();
            if (r1 == null || r2 == null) return false;
            return r1.ParagraphIndex == r2.ParagraphIndex;
        }
    }

    public static ReaderFragment newInstance(Uri uri) {
        ReaderFragment fragment = new ReaderFragment();
        Bundle args = new Bundle();
        args.putParcelable("uri", uri);
        fragment.setArguments(args);
        return fragment;
    }

    public static ReaderFragment newInstance(Uri uri, FBReaderView.ZLTextIndexPosition pos) {
        ReaderFragment fragment = new ReaderFragment();
        Bundle args = new Bundle();
        args.putParcelable("uri", uri);
        args.putParcelable("pos", pos);
        fragment.setArguments(args);
        return fragment;
    }

    public ReaderFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //keyboardHeightProvider = new KeyboardHeightProvider(requireActivity());
        // keyboardHeightProvider.addKeyboardListener(this);
        storage = new Storage(getContext());
        setHasOptionsMenu(true);
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        shared.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new Handler().postDelayed(() -> {
            if (fb != null && fb.app != null) {
                mNavigationSeekbar = new NavigationSeekbar(fb.app, requireContext());
                if (mNavigationSeekbar.getRootView() != null) {
                    mView.removeView(mNavigationSeekbar.getRootView());
                }
                mView.addView(mNavigationSeekbar.getRootView());
                fb.app.setListenZLApplication(new ZLApplication.ListenZLApplication() {
                    @Override
                    public void loadPage() {
                        mNavigationSeekbar.setUpNavigation();
                    }
                });
                mNavigationSeekbar.setListenNaviSeekbar(new NavigationSeekbar.ListenNaviSeekbar() {
                    @Override
                    public void actionReloadBookmark() {
                        fb.setUpGimBookMark();
                    }

                    @Override
                    public void actionShowKeyboard() {

                    }
                });
            }
        }, 100);
    }

    @SuppressLint("RestrictedApi")
    public void popBackStack(String tag, int flags) { // only pop existing TAG
        FragmentManager fm = requireActivity().getSupportFragmentManager();
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

    public FragmentTransaction addFragment(Fragment f, String tag) {
        return openFragment(f, tag).addToBackStack(tag);
    }

    public FragmentTransaction openFragment(Fragment f, String tag) {
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        return fm.beginTransaction().add(R.id.main_content, f, tag);
    }

    public void openNote() {
        mMainViewModel.eventLoadFrNote.setValue(true);
        mMainViewModel.eventOpenNote.setValue(true);
        popBackStack(FragmentBookMark.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        addFragment(new FragmentBookMark(fb.book.info.bookmarks, fb, 0),
                FragmentBookMark.TAG).commit();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        final View v = inflater.inflate(R.layout.fragment_reader, container, false);

        final BookActivity main = (BookActivity) getActivity();
        fb = (FBReaderView) v.findViewById(R.id.main_view);
        mViewKeyboard = v.findViewById(R.id.viewKeyboard);
        mView = v.findViewById(R.id.layoutPanel);
        fb.setListenFBReaderView(new FBReaderView.ListenFBReaderView() {
            @Override
            public void actionFull(boolean check) {
                if (check) {
                    ((BookActivity) requireActivity()).showHideToolbar(true);
                } else {
                    ((BookActivity) requireActivity()).showHideToolbar(false);
                }
            }

            @Override
            public void actionAddBookMark(boolean check) {
                //                if (fb != null && fb.app != null && fb.app.getCurrentTOCElement() != null) {
                //                    fb.app.getCurrentTOCElement().setAddBookMark(true);
                //                }
                if (fb != null) {
                    fb.setAddBookMark(true);
                }
                mMainViewModel.eventAddBookMark.setValue(check);
            }

            @Override
            public void actionNoneAddBookMark(boolean check) {
                //                if (fb != null && fb.app != null && fb.app.getCurrentTOCElement() != null) {
                //                    fb.app.getCurrentTOCElement().setAddBookMark(false);
                //                }
                if (fb != null) {
                    fb.setAddBookMark(false);
                }
                Log.d("ducNQ", "actionNoneAddBookMark: ");
                mMainViewModel.eventAddBookMark.setValue(check);
            }
        });
        ((BookActivity) requireActivity()).setListenAction(new BookActivity.ListenAction() {
            @Override
            public void actionFullScreen() {

            }

            @Override
            public void actionMucLuc() {
                if (fb.app.getCurrentTOCElement() == null) {
                    openNote();
                } else {
                    final TOCTree current = fb.app.getCurrentTOCElement();
                    List<TOCTree> tocTreeList = fb.app.Model.mTOCTree.subtrees();
                    final TOCAdapter a = new TOCAdapter(tocTreeList, current);
                    mDialogBookFragment = new DialogBookFragment(a, fb.book.info.bookmarks, fb);
                    mDialogBookFragment.show(getParentFragmentManager(), DialogBookFragment.TAG);
                }
            }

            @Override
            public void actionFont(ImageView imageView) {
                setFont(imageView);
            }

            @Override
            public void actionBookMark() {
                if (fb != null && fb.app != null && fb.book.info != null) {
                    if (fb.book.info.bookmarks == null) {
                        fb.book.info.bookmarks = new Storage.Bookmarks();
                    }
                    boolean isAddBookMark =
                            fb.isAddBookMark();//fb.app.getCurrentTOCElement().isAddBookMark();
                    //check khi book có mục lục
                    if (fb.app.getCurrentTOCElement() != null) {
                        //check xem đã đc add vào bookmark hay chưa
                        if (!isAddBookMark) {
                            String nameBookMark = fb.app.getCurrentTOCElement().getText();
                            final ZLTextView textView = fb.app.getTextView();
                            final ZLTextView.PagePosition pagePosition = textView.pagePosition();
                            Storage.Bookmark bookmark =
                                    new Storage.Bookmark(nameBookMark, pagePosition.Current,
                                            TYPE_BOOKMARK, pagePosition.Total);
                            fb.book.info.bookmarks.add(bookmark);
                            fb.setTimeBookMark(bookmark.last);
                            fb.setAddBookMark(true);
                            // fb.app.getCurrentTOCElement().setTimeBookMark(bookmark.last);
                            // fb.app.getCurrentTOCElement().setAddBookMark(true);
                            mMainViewModel.eventAddBookMark.setValue(true);
                        } else {
                            removeBookmark();
                        }
                    } else {// khi book không có mục lục
                        if (!isAddBookMark) {
                            final ZLTextView textView = fb.app.getTextView();
                            final ZLTextView.PagePosition pagePosition = textView.pagePosition();
                            Storage.Bookmark bookmark = new Storage.Bookmark(
                                    pagePosition.Current + "/" + pagePosition.Total,
                                    pagePosition.Current, TYPE_BOOKMARK, pagePosition.Total);
                            fb.book.info.bookmarks.add(bookmark);
                            fb.setTimeBookMark(bookmark.last);
                            fb.setAddBookMark(true);
                            mMainViewModel.eventAddBookMark.setValue(true);
                        } else {
                            removeBookmark();
                        }
                    }
                }
            }

            @Override
            public void extendView(ImageView imageView) {
                btnBookMark = imageView;
            }

            @Override
            public void actionTTS() {
                setUpTTS();
                final ZLTextView textView = fb.app.getTextView();
                final ZLTextView.PagePosition pagePosition = textView.pagePosition();

                Log.d("ducNQ", "actionFont: " + pagePosition.Current);
                Log.d("ducNQ", "actionFont: " + pagePosition.Total);
            }
        });
        //initVariable(v);
        // initAction();

        fb.listener = new FBReaderView.Listener() {
            @Override
            public void onScrollingFinished(ZLViewEnums.PageIndex index) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fb.setUpGimBookMark();
                    }
                }, 100);

                if (fontsPopup != null) {
                    fontsPopup.dismiss();
                    fontsPopup = null;
                }
                updateToolbar();
            }

            @Override
            public void onSearchClose() {
                MenuItemCompat.collapseActionView(searchMenu);
            }

            @Override
            public void onBookmarksUpdate() {
                updateToolbar();
                //                if (fb.book != null) {
                //                    mMainViewModel.eventBookMark.setValue(
                //                            fb.book.info.bookmarks != null && fb.book.info.bookmarks.size() > 0);
                //                }
            }

            @Override
            public void onDismissDialog() {
                if (main.fullscreen) main.hideSystemUI();
            }

            @Override
            public void ttsStatus(boolean speaking) {
                BookActivity main = (BookActivity) getActivity();
                main.volumeEnabled = !speaking;
            }
        };

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        String mode = shared.getString(BookApplication.PREFERENCE_VIEW_MODE, "");
        fb.setWidget(mode.equals(FBReaderView.Widgets.CONTINUOUS.toString())
                ? FBReaderView.Widgets.CONTINUOUS : FBReaderView.Widgets.PAGING);

        fb.setWindow(getActivity().getWindow());
        fb.setActivity(getActivity());

        Uri uri = getArguments().getParcelable("uri");
        FBReaderView.ZLTextIndexPosition pos = getArguments().getParcelable("pos");

        try {
            book = storage.load(uri);
            fbook = storage.read(book);
            fb.loadBook(fbook);
            if (pos != null) fb.gotoPosition(pos);
        } catch (RuntimeException e) {
            ErrorDialog.Error(main, e);
            handler.post(
                    new Runnable() { // or openLibrary crash with java.lang.IllegalStateException on FragmentActivity.onResume
                        @Override
                        public void run() {
                            if (!main.isFinishing()) main.openLibrary();
                        }
                    });
            return v; // ignore post called
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (getActivity().isFinishing()) return;
                updateToolbar(); // update toolbar after page been drawn to detect RTL
                fb.showControls(); //  update toolbar after page been drawn, getWidth() == 0
            }
        });
        fb.setUpGimBookMark();
        //        new Handler().postDelayed(new Runnable() {
        //            @Override
        //            public void run() {
        //                fb.setUpGimBookMark();
        //            }
        //        }, 100);

        return v;
    }

    private void removeBookmark() {
        Storage.Bookmark bookmark = fb.getCurrentBookMark();
        if (bookmark != null) {
            if (fb.book.info.bookmarks == null) return;
            int i = fb.book.info.bookmarks.indexOf(bookmark);
            if (i >= 0 && i < fb.book.info.bookmarks.size()) {
                fb.book.info.bookmarks.remove(i);
                //  i = fb.book.info.bookmarks.indexOf(bookmark);
                //fb.book.info.bookmarks.remove(i);
                fb.bookmarksUpdate();
                savePosition();
                mMainViewModel.eventAddBookMark.setValue(false);
                fb.setAddBookMark(false);
                //fb.app.getCurrentTOCElement().setAddBookMark(false);
            }
        }
    }

    void updateToolbar() {
        if (invalidateOptionsMenu != null) invalidateOptionsMenu.run();
    }

    @Override
    public void onResume() {
        super.onResume();
        ScreenlockPreference.onResume(getActivity(), BookApplication.PREFERENCE_SCREENLOCK);

        battery = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                fb.battery = level * 100 / scale;
                fb.invalidateFooter();
            }
        };
        battery.onReceive(getContext(), getContext().registerReceiver(battery,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));

        time.run();

        updateTheme(); // MainActivity.restartActivity() not called when double change while ReaderFragment active
    }

    @Override
    public void onPause() {
        super.onPause();
        savePosition();
        ScreenlockPreference.onPause(getActivity(), BookApplication.PREFERENCE_SCREENLOCK);

        if (battery != null) {
            getContext().unregisterReceiver(battery);
            battery = null;
        }

        handler.removeCallbacks(time);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        savePosition();
    }

    public float getFontsizeReflow() {
        Float fontsize = fb.getFontsizeReflow();
        if (fontsize != null) return fontsize;
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        return shared.getFloat(BookApplication.PREFERENCE_FONTSIZE_REFLOW,
                BookApplication.PREFERENCE_FONTSIZE_REFLOW_DEFAULT);
    }

    void savePosition() {
        if (book == null) return;
        if (fb.book == null) // when book isn't loaded and view closed
        {
            return;
        }
        Storage.RecentInfo save = new Storage.RecentInfo(fb.book.info);
        save.position = fb.getPosition();
        Uri u = storage.recentUri(book);
        if (Storage.exists(getContext(),
                u)) { // file can be changed during sync, check for conflicts
            try {
                Storage.RecentInfo info = new Storage.RecentInfo(getContext(), u);
                if (info.position != null && save.position.samePositionAs(info.position)) {
                    if (save.fontsize == null || info.fontsize != null && save.fontsize.equals(
                            info.fontsize)) {
                        if (save.equals(info.fontsizes)) {
                            if (save.bookmarks == null
                                    || info.bookmarks != null && save.bookmarks.equals(
                                    info.bookmarks)) {
                                return; // nothing to save
                            }
                        }
                    }
                }
                if (book.info.last != info.last) // file changed between saves?
                {
                    storage.move(u, storage.getStoragePath()); // yes. create conflict (1)
                }
                save.merge(info.fontsizes, info.last);
            } catch (RuntimeException e) {
                Log.d(TAG, "Unable to load JSON", e);
            }
        }
        book.info = save;
        storage.save(book);
        Log.d(TAG, "savePosition " + save.position);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        shared.unregisterOnSharedPreferenceChangeListener(this);
        handler.removeCallbacks(time);
        ScreenlockPreference.onUserInteractionRemove();
        if (fb != null) // onDestory without onCreate
        {
            fb.closeBook();
        }
        if (fontsPopup != null) {
            fontsPopup.dismiss();
            fontsPopup = null;
        }
        if (fbook != null) {
            fbook.close();
            fbook = null;
        }
        book = null;
    }

    @Override
    public void onUserInteraction() {
        ScreenlockPreference.onUserInteraction(getActivity(),
                BookApplication.PREFERENCE_SCREENLOCK);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (fontsPopup != null) {
            fontsPopup.dismiss();
            fontsPopup = null;
        }
        int id = item.getItemId();
        if (id == R.id.action_toc) {
            showTOC();
            return true;
        }
        if (id == R.id.action_bm) {
            BookmarksDialog dialog = new BookmarksDialog(getContext()) {
                @Override
                public void onSelected(Storage.Bookmark b) {
                    fb.gotoPosition(new FBReaderView.ZLTextIndexPosition(b.start, b.end));
                }

                @Override
                public void onSave(Storage.Bookmark bm) {
                    fb.bookmarksUpdate();
                    savePosition();
                }

                @Override
                public void onDelete(Storage.Bookmark bm) {
                    int i = book.info.bookmarks.indexOf(bm);
                    book.info.bookmarks.remove(i);
                    i = fb.book.info.bookmarks.indexOf(bm);
                    fb.book.info.bookmarks.remove(i);
                    fb.bookmarksUpdate();
                    savePosition();
                }
            };
            dialog.load(fb.book.info.bookmarks);
            dialog.show();
            return true;
        }
        //        if (id == R.id.action_reflow) {
        //            fb.setReflow(!fb.isReflow());
        //            updateToolbar();
        //        }
        if (id == R.id.action_debug) {
            fb.pluginview.reflowDebug = !fb.pluginview.reflowDebug;
            if (fb.pluginview.reflowDebug) {
                fb.pluginview.reflow = true;
                fb.setWidget(FBReaderView.Widgets.PAGING);
            }
            fb.reset();
            updateToolbar();
        }
        if (id == R.id.action_fontsize) {
            if (fb.pluginview == null) {
                fontsPopup = new FontsPopup(getContext(), BookApplication.from(getContext()).ttf) {
                    @Override
                    public void setFont(String f) {
                        SharedPreferences shared =
                                PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor edit = shared.edit();
                        edit.putString(BookApplication.PREFERENCE_FONTFAMILY_FBREADER, f);
                        edit.apply();
                        fb.setFontFB(f);
                        updateToolbar();
                    }

                    @Override
                    public void setFontsize(int f) {
                        SharedPreferences shared =
                                PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor edit = shared.edit();
                        edit.putInt(BookApplication.PREFERENCE_FONTSIZE_FBREADER, f);
                        edit.apply();
                        fb.setFontsizeFB(f);
                        updateToolbar();
                    }

                    @Override
                    public void setIgnoreEmbeddedFonts(boolean f) {
                        SharedPreferences shared =
                                PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor edit = shared.edit();
                        edit.putBoolean(BookApplication.PREFERENCE_IGNORE_EMBEDDED_FONTS, f);
                        edit.apply();
                        fb.setIgnoreCssFonts(f);
                        updateToolbar();
                    }

                    @Override
                    public void updateFontsize(int f) {
                        fontsizepopup_text.setText(Integer.toString(f));
                    }
                };
                fontsPopup.fragment = this;
                fontsPopup.code = RESULT_FONTS;
                fontsPopup.loadFonts();
                fontsPopup.fonts.select(fb.app.ViewOptions.getTextStyleCollection()
                        .getBaseStyle().FontFamilyOption.getValue());
                fontsPopup.ignore_embedded_fonts.setChecked(fb.getIgnoreCssFonts());
                fontsPopup.fontsList.scrollToPosition(fontsPopup.fonts.selected);
                fontsPopup.updateFontsize(FONT_START, FONT_END, fb.getFontsizeFB());
            } else {
                fontsPopup = new FontsPopup(getContext(), BookApplication.from(getContext()).ttf) {
                    @Override
                    public void setFontsize(int f) {
                        float p = f / 10f;
                        SharedPreferences shared =
                                PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor editor = shared.edit();
                        editor.putFloat(BookApplication.PREFERENCE_FONTSIZE_REFLOW, p);
                        editor.apply();
                        fb.setFontsizeReflow(p);
                        updateToolbar();
                    }

                    @Override
                    public void updateFontsize(int f) {
                        fontsizepopup_text.setText(String.format("%.1f", f / 10f));
                    }
                };
                fontsPopup.fontsFrame.setVisibility(View.GONE);
                fontsPopup.updateFontsize(REFLOW_START, REFLOW_END,
                        (int) (getFontsizeReflow() * 10));
            }
            View v = MenuItemCompat.getActionView(item);
            if (v == null || !ViewCompat.isAttachedToWindow(v)) {
                v = getOverflowMenuButton(getActivity());
            }
            PopupWindowCompat.showAsTooltip(fontsPopup, v, Gravity.BOTTOM,
                    ThemeUtils.getThemeColor(getContext(), R.attr.colorButtonNormal),
                    // v has overflow ThemedContext
                    ThemeUtils.dp2px(getContext(), 300));
        }
        if (id == R.id.action_rtl) {
            fb.app.BookTextView.rtlMode = !fb.app.BookTextView.rtlMode;
            fb.reset();
            updateToolbar();
        }
        if (id == R.id.action_mode) {
            FBReaderView.Widgets m = fb.widget instanceof ScrollWidget ? FBReaderView.Widgets.PAGING
                    : FBReaderView.Widgets.CONTINUOUS;
            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor edit = shared.edit();
            edit.putString(BookApplication.PREFERENCE_VIEW_MODE, m.toString());
            edit.apply();
            fb.setWidget(m);
            fb.reset();
            updateToolbar();
        }
        if (id == R.id.action_tts) {
            if (fb.tts != null) {
                fb.tts.dismiss();
                fb.tts = null;
            } else {
                fb.ttsOpen();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void setUpTTS() {
        if (fb.tts != null) {
            fb.tts.dismiss();
            fb.tts = null;
        } else {
            fb.ttsOpen();
        }
    }

    private void setFont(ImageView v) {
        if (fb.pluginview == null) {
            fontsPopup = new FontsPopup(getContext(), BookApplication.from(getContext()).ttf) {
                @Override
                public void setFont(String f) {
                    SharedPreferences shared =
                            PreferenceManager.getDefaultSharedPreferences(getContext());
                    SharedPreferences.Editor edit = shared.edit();
                    edit.putString(BookApplication.PREFERENCE_FONTFAMILY_FBREADER, f);
                    edit.apply();
                    fb.setFontFB(f);
                    updateToolbar();
                }

                @Override
                public void setFontsize(int f) {
                    SharedPreferences shared =
                            PreferenceManager.getDefaultSharedPreferences(getContext());
                    SharedPreferences.Editor edit = shared.edit();
                    edit.putInt(BookApplication.PREFERENCE_FONTSIZE_FBREADER, f);
                    edit.apply();
                    fb.setFontsizeFB(f);
                    updateToolbar();
                }

                @Override
                public void setIgnoreEmbeddedFonts(boolean f) {
                    SharedPreferences shared =
                            PreferenceManager.getDefaultSharedPreferences(getContext());
                    SharedPreferences.Editor edit = shared.edit();
                    edit.putBoolean(BookApplication.PREFERENCE_IGNORE_EMBEDDED_FONTS, f);
                    edit.apply();
                    fb.setIgnoreCssFonts(f);
                    updateToolbar();
                }

                @Override
                public void updateFontsize(int f) {
                    fontsizepopup_text.setText(Integer.toString(f));
                }
            };
            fontsPopup.fragment = this;
            fontsPopup.code = RESULT_FONTS;
            fontsPopup.loadFonts();
            fontsPopup.fonts.select(fb.app.ViewOptions.getTextStyleCollection()
                    .getBaseStyle().FontFamilyOption.getValue());
            fontsPopup.ignore_embedded_fonts.setChecked(fb.getIgnoreCssFonts());
            fontsPopup.fontsList.scrollToPosition(fontsPopup.fonts.selected);
            fontsPopup.updateFontsize(FONT_START, FONT_END, fb.getFontsizeFB());
        } else {
            fontsPopup = new FontsPopup(getContext(), BookApplication.from(getContext()).ttf) {
                @Override
                public void setFontsize(int f) {
                    float p = f / 10f;
                    SharedPreferences shared =
                            PreferenceManager.getDefaultSharedPreferences(getContext());
                    SharedPreferences.Editor editor = shared.edit();
                    editor.putFloat(BookApplication.PREFERENCE_FONTSIZE_REFLOW, p);
                    editor.apply();
                    fb.setFontsizeReflow(p);
                    updateToolbar();
                }

                @Override
                public void updateFontsize(int f) {
                    fontsizepopup_text.setText(String.format("%.1f", f / 10f));
                }
            };
            fontsPopup.fontsFrame.setVisibility(View.GONE);
            fontsPopup.updateFontsize(REFLOW_START, REFLOW_END, (int) (getFontsizeReflow() * 10));
        }
        //        View v = MenuItemCompat.getActionView(item);
        //        if (v == null || !ViewCompat.isAttachedToWindow(v)) {
        //            v = getOverflowMenuButton(getActivity());
        //        }
        PopupWindowCompat.showAsTooltip(fontsPopup, v, Gravity.BOTTOM,
                ThemeUtils.getThemeColor(getContext(), R.attr.colorButtonNormal),
                // v has overflow ThemedContext
                ThemeUtils.dp2px(getContext(), 300));
    }

    private void setUpBookMark() {
        if (fb == null) return;
        BookmarksDialog dialog = new BookmarksDialog(getContext()) {
            @Override
            public void onSelected(Storage.Bookmark b) {
                fb.gotoPosition(new FBReaderView.ZLTextIndexPosition(b.start, b.end));
            }

            @Override
            public void onSave(Storage.Bookmark bm) {
                fb.bookmarksUpdate();
                savePosition();
            }

            @Override
            public void onDelete(Storage.Bookmark bm) {
                if (book.info.bookmarks == null) return;
                int i = book.info.bookmarks.indexOf(bm);
                if (i >= 0 && i < book.info.bookmarks.size()) {
                    book.info.bookmarks.remove(i);
                    i = fb.book.info.bookmarks.indexOf(bm);
                    fb.book.info.bookmarks.remove(i);
                    fb.bookmarksUpdate();
                    savePosition();
                }
            }
        };
        dialog.load(fb.book.info.bookmarks);
        dialog.show();
    }

    public void updateTheme() {
        fb.updateTheme();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // invalidateOptionsMenu =
        //        InvalidateOptionsMenuCompat.onCreateOptionsMenu(this, menu, inflater);

        MenuItem homeMenu = menu.findItem(R.id.action_home);
        MenuItem tocMenu = menu.findItem(R.id.action_toc);
        searchMenu = menu.findItem(R.id.action_search);
        //MenuItem reflow = menu.findItem(R.id.action_reflow);
        // MenuItem debug = menu.findItem(R.id.action_debug);
        MenuItem bookmarksMenu = menu.findItem(R.id.action_bm);
        final MenuItem fontsize = menu.findItem(R.id.action_fontsize);
        final MenuItem rtl = menu.findItem(R.id.action_rtl);
        MenuItem grid = menu.findItem(R.id.action_grid);
        MenuItem mode = menu.findItem(R.id.action_mode);
        MenuItem theme = menu.findItem(R.id.action_theme);
        MenuItem sort = menu.findItem(R.id.action_sort);
        MenuItem tts = menu.findItem(R.id.action_tts);

        boolean search;

        if (fb.pluginview == null) {
            search = true;
        } else {
            Plugin.View.Search s = fb.pluginview.search("");
            if (s == null) {
                search = false;
            } else {
                s.close();
                search = true;
            }
            if (fb.pluginview.reflow || fb.pluginview instanceof ComicsPlugin.ComicsView) {
                tts.setVisible(false); // TODO reflow - possible and can be very practical
            }
        }

        grid.setVisible(false);
        homeMenu.setVisible(false);
        sort.setVisible(false);
        tocMenu.setVisible(fb.app.Model != null
                && fb.app.Model.mTOCTree != null
                && fb.app.Model.mTOCTree.hasChildren());
        //        mMainViewModel.eventShowMucLuc.setValue(fb.app.Model != null
        //                && fb.app.Model.TOCTree != null
        //                && fb.app.Model.TOCTree.hasChildren());
        searchMenu.setVisible(search);
        mMainViewModel.eventSearchN.setValue(search);
        //reflow.setVisible(
        //     fb.pluginview != null && !(fb.pluginview instanceof ComicsPlugin.ComicsView));

      /*  if (BuildConfig.DEBUG
                && fb.pluginview != null
                && !(fb.pluginview instanceof ComicsPlugin.ComicsView)) {
            //  debug.setVisible(true);
        } else {
            //  debug.setVisible(false);
        }*/

        fontsize.setVisible(fb.pluginview == null || fb.pluginview.reflow);
        mMainViewModel.eventFont.setValue(fb.pluginview == null || fb.pluginview.reflow);
        if (fb.pluginview == null) {
            ((ToolbarButtonView) MenuItemCompat.getActionView(fontsize)).text.setText(
                    "" + (fb.book == null ? "" : fb.getFontsizeFB())); // call before onCreateView
        } else {
            ((ToolbarButtonView) MenuItemCompat.getActionView(fontsize)).text.setText(
                    String.format("%.1f", getFontsizeReflow()));
        }
        MenuItemCompat.getActionView(fontsize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(fontsize);
            }
        });

        mode.setIcon(fb.widget instanceof ScrollWidget ? R.drawable.ic_view_day_black_24dp
                : R.drawable.ic_view_carousel_black_24dp); // icon current
        mode.setTitle(fb.widget instanceof ScrollWidget ? R.string.view_mode_paging
                : R.string.view_mode_continuous); // text next

        showRTL |= !fb.app.BookTextView.rtlMode && fb.app.BookTextView.rtlDetected;
        if (showRTL) {
            rtl.setVisible(true);
        } else {
            rtl.setVisible(false);
        }
        MenuItemCompat.getActionView(rtl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(rtl);
            }
        });
        rtl.setTitle(fb.app.BookTextView.rtlMode ? "RTL" : "LTR");
        ((ToolbarButtonView) MenuItemCompat.getActionView(rtl)).text.setText(
                fb.app.BookTextView.rtlMode ? "RTL" : "LTR");

        if (fb.book != null) // call before onCreateView
        {
            bookmarksMenu.setVisible(
                    fb.book.info.bookmarks != null && fb.book.info.bookmarks.size() > 0);
            //            mMainViewModel.eventBookMark.setValue(
            //                    fb.book.info.bookmarks != null && fb.book.info.bookmarks.size() > 0);
        }

        if (fb.pluginview instanceof ComicsPlugin.ComicsView) theme.setVisible(false);
        //setUpView();
    }

    void showTOC() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final TOCTree current = fb.app.getCurrentTOCElement();
        final TOCAdapter a = new TOCAdapter(fb.app.Model.mTOCTree.subtrees(), current);
        final TreeRecyclerView tree = new TreeRecyclerView(getContext());
        tree.setAdapter((RecyclerView.Adapter) a);
        builder.setView(tree);
        builder.setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        tocdialog = builder.create();
        tocdialog.setOnShowListener(dialog -> {
            final int i = a.getCurrent() - 1;
            if (i > 0) tree.setSelection(i);
        });
        tocdialog.show();
    }

    @Override
    public void search(String s) {
        fb.app.runAction(ActionCode.SEARCH, s);
    }

    @Override
    public void searchClose() {
        fb.searchClose();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(BookApplication.PREFERENCE_VIEW_MODE)) {
            fb.configWidget(sharedPreferences);
            fb.showControls();
        }
        if (key.equals(BookApplication.PREFERENCE_THEME)) {
            fb.configColorProfile(sharedPreferences);
        }
    }

    @Override
    public String getHint() {
        return getString(R.string.search_book);
    }

    @Override
    public void onFullscreenChanged(boolean f) {
        fb.onConfigurationChanged(null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            fb.app.runAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD);
            return true;
        }
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            fb.app.runAction(ActionCode.VOLUME_KEY_SCROLL_BACK);
            return true;
        }
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) return true;
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)) return true;
        return false;
    }

    @Override
    public boolean onBackPressed() {
        Log.d("ducNQ", "onBackPressed: " + fb.isPinch());
        if (fb.isPinch()) {
            fb.pinchClose();
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (fontsPopup != null && fontsPopup.choicer != null) {
            fontsPopup.choicer.onActivityResult(resultCode, data);
        }
    }

    /*@Override
    public void onHeightChanged(int height, int lastHeight) {
        Log.d("ducNQ", "onHeightChanged: ");
        int halfHeightDevice = Util.getScreenHeight(requireActivity()) / 2;
        ConstraintLayout.LayoutParams layoutParamsMainLayout =
                (ConstraintLayout.LayoutParams) mViewKeyboard.getLayoutParams();
        if (height > 0) {
           *//* if (height + Util.getHeightNavigationBar(requireActivity()) > halfHeightDevice) {
                layoutParamsMainLayout.height = height;
            } else {
                layoutParamsMainLayout.height =
                        height + Util.getHeightNavigationBar(requireActivity());
            }*//*
            layoutParamsMainLayout.height = height;
            if (mNavigationSeekbar != null) {
                mNavigationSeekbar.refreshEdittext();
            }
        } else {
            layoutParamsMainLayout.height = 1;
            if (mNavigationSeekbar != null) {
                //mNavigationSeekbar.showTextPage();
            }
        }
        mViewKeyboard.requestLayout();
    }

    @Override
    public void onHeightNoChanged(int height, int lastHeight) {

    }*/
}
