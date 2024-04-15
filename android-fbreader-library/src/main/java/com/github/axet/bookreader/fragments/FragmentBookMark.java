package com.github.axet.bookreader.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.axet.bookreader.adapter.BookMarkNewAdapter;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.viewmodel.MainViewModel;
import com.github.axet.bookreader.widgets.FBReaderView;
import java.util.ArrayList;
import java.util.List;
import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.databinding.FragmentBookmarkBinding;

import static com.github.axet.bookreader.app.Storage.TYPE_NOTE;

public class FragmentBookMark extends Fragment {
    private List<Storage.Bookmark> mBookmarkList = new ArrayList<>();
    private FragmentBookmarkBinding binding;
    private FBReaderView mFBReaderView;
    MainViewModel mMainViewModel;
    private int mType = 1;
    public static final String TAG = "FragmentBookMark";

    public FragmentBookMark() {

    }

    public FragmentBookMark(List<Storage.Bookmark> bookmarkList, FBReaderView fbReaderView,
            int type) {
        mBookmarkList = bookmarkList;
        mFBReaderView = fbReaderView;
        mType = type;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_bookmark, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        observerData();
        //        if (mType == 1) {
        //            binding.group.setVisibility(View.GONE);
        //        }
        //        binding.imgBack.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void setUpAdapter() {
        if (mBookmarkList == null) return;
        BookMarkNewAdapter bookMarkNewAdapter = new BookMarkNewAdapter(mFBReaderView);
        bookMarkNewAdapter.setTypeHolder(0);
        bookMarkNewAdapter.setListBookMark(mBookmarkList);
        binding.rcBookMark.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        binding.rcBookMark.setAdapter(bookMarkNewAdapter);
        bookMarkNewAdapter.setActionItemBookmark(bookmark -> {
            if (mFBReaderView != null) {
                if (bookmark.type == TYPE_NOTE) {
                    mFBReaderView.gotoPosition(
                            new FBReaderView.ZLTextIndexPosition(bookmark.start, bookmark.end));
                    mFBReaderView.setUpGimBookMark();
                } else {
                    if (mFBReaderView.app != null) {
                        mFBReaderView.app.getTextView().gotoPage(bookmark.pageBookMark);
                        mMainViewModel.eventAddBookMark.setValue(true);
                    }
                }
                if (mType == 0) {
                    mMainViewModel.eventOpenNote.setValue(false);
                    requireActivity().onBackPressed();
                } else {
                    mMainViewModel.eventCloseBookMark.setValue(true);
                }
            }
            return null;
        });
    }

    private void observerData() {
        mMainViewModel.eventLoadFrNote.observe(getViewLifecycleOwner(), aBoolean -> {
            if (aBoolean) {
                new Handler().postDelayed(() -> {
                    setUpAdapter();
                    binding.prLoading.setVisibility(View.INVISIBLE);
                }, 300);
            }
        });
    }
}
