/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader;

import android.animation.*;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.constraintlayout.widget.ConstraintLayout;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.R;

public class NavigationWindow extends LinearLayout {
    public View rootView;
    private FBReaderApp myFBReader;
    private ListenNaviWindow mListenNaviWindow;
    private volatile boolean myIsInProgress;

    public void setListenNaviWindow(ListenNaviWindow listenNaviWindow) {
        mListenNaviWindow = listenNaviWindow;
    }

    public NavigationWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavigationWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private Animator myShowHideAnimator;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    public void show() {
        post(new Runnable() {
            public void run() {
                setVisibility(View.VISIBLE);
            }
        });
    }

    public void hide() {
        post(new Runnable() {
            public void run() {
                setVisibility(View.GONE);
            }
        });
    }

    public interface ListenNaviWindow {
        void actionSeekbar(SeekBar seekBar, int progress);
    }
}
