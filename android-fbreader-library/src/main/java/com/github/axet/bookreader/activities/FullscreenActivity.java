package com.github.axet.bookreader.activities;

import android.annotation.SuppressLint;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.github.axet.androidlibrary.activities.AppCompatFullscreenThemeActivity;
import com.github.axet.androidlibrary.widgets.SearchView;
import com.github.axet.bookreader.app.BookApplication;
import com.github.axet.bookreader.viewmodel.MainViewModel;
import java.util.List;
import org.geometerplus.zlibrary.ui.android.R;

public class FullscreenActivity extends AppCompatFullscreenThemeActivity {
    public Toolbar toolbar;
    protected ConstraintLayout layoutMenu;
    protected ImageView btnTTS, btnBookMark, btnFont, btnMucLuc, btnSearch, btnCloseSearch,
            btnOption;
    protected SearchView actionSearch;
    private ListenAction mListenAction;
    protected ImageView btnBack;
    protected Group mGroup;
    protected MainViewModel mMainViewModel;
    public static final String PATH_BOOK = "PATH_BOOK";
    public interface FullscreenListener {
        void onFullscreenChanged(boolean f);

        void onUserInteraction();
    }
    public static Intent newInstance(Context context, String pathBook) {
        Intent intent = new Intent(context, FullscreenActivity.class);
        intent.putExtra(PATH_BOOK, pathBook);
        return intent;
    }
    public void setListenAction(ListenAction listenAction) {
        mListenAction = listenAction;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ducNQ", "onCreateFullscreenActivity: ");
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
        String pathBook = getIntent().getStringExtra(PATH_BOOK);
        startActivity(BookActivity.newInstance(this,pathBook));
        setOnClick();
        observerData();
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

    @Override
    protected void onResume() {
        super.onResume();
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

    @SuppressLint({ "InlinedApi", "RestrictedApi" })
    @Override
    public void setFullscreen(boolean b) {
        super.setFullscreen(b);
        FragmentManager fm = getSupportFragmentManager();
        List<Fragment> ff = fm.getFragments();
        if (ff != null) {
            for (Fragment f : ff) {
                if (f instanceof FullscreenListener) {
                    ((FullscreenListener) f).onFullscreenChanged(b);
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
                if (f instanceof FullscreenListener) ((FullscreenListener) f).onUserInteraction();
            }
        }
    }

    public interface ListenAction {
        void actionFullScreen();

        void actionMucLuc();

        void actionFont(ImageView imageView);

        void actionBookMark();

        void extendView(ImageView imageView);

        void actionTTS();
    }
}
