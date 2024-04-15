package com.github.axet.bookreader.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.fragments.FragmentBookMark;
import com.github.axet.bookreader.fragments.FragmentTableOfContent;
import com.github.axet.bookreader.fragments.ReaderFragment;
import com.github.axet.bookreader.widgets.FBReaderView;
import java.util.ArrayList;
import java.util.List;
import org.geometerplus.fbreader.bookmodel.TOCTree;

public class ViewPagerAdapters extends FragmentStateAdapter {
    private TOCTree mTOCTree;
    private List<TOCTree> mTOCTreeList;
    private FBReaderView mFBReaderView;
    private ReaderFragment.TOCAdapter mTOCAdapter;
    private List<Storage.Bookmark> mBookmarkList = new ArrayList<>();

    public ViewPagerAdapters(@NonNull FragmentActivity fragmentActivity, TOCTree TOCTree,
            List<TOCTree> TOCTreeList, ReaderFragment.TOCAdapter TOCAdapter) {
        super(fragmentActivity);
        mTOCTree = TOCTree;
        mTOCTreeList = TOCTreeList;
        mTOCAdapter = TOCAdapter;
    }

    public ViewPagerAdapters(@NonNull FragmentActivity fragmentActivity,
            ReaderFragment.TOCAdapter TOCAdapter, List<Storage.Bookmark> bookmarkList,
            FBReaderView fbReaderView) {
        super(fragmentActivity);
        mTOCAdapter = TOCAdapter;
        mBookmarkList = bookmarkList;
        mFBReaderView = fbReaderView;
    }

    public ViewPagerAdapters(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                FragmentTableOfContent fragmentTableOfContent =
                        new FragmentTableOfContent(mTOCAdapter);
                return fragmentTableOfContent;
            default:
                FragmentBookMark fragmentBookMark =
                        new FragmentBookMark(mBookmarkList, mFBReaderView,1);
                return fragmentBookMark;
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
