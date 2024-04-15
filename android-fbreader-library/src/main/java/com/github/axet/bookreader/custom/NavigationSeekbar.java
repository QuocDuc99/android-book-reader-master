package com.github.axet.bookreader.custom;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import com.github.axet.bookreader.dialog.DialogPage;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor;

public class NavigationSeekbar {
    private View rootView;
    private final FBReaderApp myFBReader;
    private ZLTextWordCursor myStartPosition;
    private volatile boolean myIsInProgress;
    private ListenNaviSeekbar mListenNaviSeekbar;
    private int progressCurrent;
    private boolean fromUserCurrent;
    private SeekBar slider;
    private TextView text;
    private Context mContext;

    public NavigationSeekbar(FBReaderApp myFBReader, Context context) {
        this.myFBReader = myFBReader;
        mContext = context;
        init(context);
    }

    public View getRootView() {
        return rootView;
    }

    public void setListenNaviSeekbar(ListenNaviSeekbar listenNaviSeekbar) {
        mListenNaviSeekbar = listenNaviSeekbar;
    }

    @SuppressLint("InflateParams")
    private void init(Context context) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        rootView = inflater.inflate(org.geometerplus.zlibrary.ui.android.R.layout.navigation_panel,
                null);
        createPanel();
    }

    private void gotoPage(int page) {
        final ZLTextView view = myFBReader.getTextView();
        if (page == 1) {
            view.gotoHome();
        } else {
            view.gotoPage(page);
        }
        if (myFBReader.getViewWidget() != null) {
            myFBReader.getViewWidget().reset();
            myFBReader.getViewWidget().repaint();
        }
    }

    private void createPanel() {
        slider = (SeekBar) rootView.findViewById(
                org.geometerplus.zlibrary.ui.android.R.id.navigation_slider);
        text = (TextView) rootView.findViewById(
                org.geometerplus.zlibrary.ui.android.R.id.navigation_text);
        setUpNavigation();
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seekBar) {
                myIsInProgress = true;
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (fromUserCurrent) {
                    final int page = progressCurrent + 1;
                    final int pagesNumber = seekBar.getMax() + 1;
                    gotoPage(page);
                    text.setText(makeProgressText(page, pagesNumber));
                }
                myIsInProgress = false;
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressCurrent = progress;
                fromUserCurrent = fromUser;
            }
        });
        text.setOnClickListener(v -> {
            final ZLTextView textView = myFBReader.getTextView();
            final ZLTextView.PagePosition pagePosition = textView.pagePosition();
            DialogPage dialogPage = new DialogPage(mContext, 1, pagePosition.Total);
            dialogPage.show();
            dialogPage.setActionCancel(() -> {
                dialogPage.dismiss();
                return null;
            });
            dialogPage.setActionOk(integer -> {
                gotoPage(integer);
                dialogPage.dismiss();
                return null;
            });
        });
    }

    private int getSelectionCurrent(String s) {
        int select = s.length();
        if (s.contains("/")) {
            select = s.indexOf('/');
            return select;
        }
        return select;
    }

    public void setUpNavigation() {
        if (rootView == null) return;
        final SeekBar slider = (SeekBar) rootView.findViewById(
                org.geometerplus.zlibrary.ui.android.R.id.navigation_slider);
        final TextView text = (TextView) rootView.findViewById(
                org.geometerplus.zlibrary.ui.android.R.id.navigation_text);
        final ZLTextView textView = myFBReader.getTextView();
        final ZLTextView.PagePosition pagePosition = textView.pagePosition();

        if (slider.getMax() != pagePosition.Total - 1
                || slider.getProgress() != pagePosition.Current - 1) {
            slider.setMax(pagePosition.Total - 1);
            slider.setProgress(pagePosition.Current - 1);
            text.setText(makeProgressText(pagePosition.Current, pagePosition.Total));
        }
    }

    private String makeProgressText(int page, int pagesNumber) {
        final StringBuilder builder = new StringBuilder();
        builder.append(page);
        builder.append("/");
        builder.append(pagesNumber);
       /* final TOCTree tocElement = myFBReader.getCurrentTOCElement();
        if (tocElement != null) {
            builder.append("  ");
            builder.append(tocElement.getText());
        }*/
        if (mListenNaviSeekbar != null) {
            mListenNaviSeekbar.actionReloadBookmark();
        }
        return builder.toString();
    }

    public interface ListenNaviSeekbar {
        void actionReloadBookmark();
        void actionShowKeyboard();
    }
}
