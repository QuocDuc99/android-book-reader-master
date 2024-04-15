package com.github.axet.bookreader.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.github.axet.bookreader.adapter.ViewPagerAdapters;
import com.github.axet.bookreader.app.Storage;
import com.github.axet.bookreader.viewmodel.MainViewModel;
import com.github.axet.bookreader.widgets.FBReaderView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
import java.util.List;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.databinding.BottomSheetBinding;

public class DialogBookFragment extends DialogFragment {
    private TOCTree mTOCTree;
    private List<TOCTree> mTOCTreeList;
    private FBReaderView mFBReaderView;
    private ReaderFragment.TOCAdapter mTOCAdapter;
    private List<Storage.Bookmark> mBookmarkList = new ArrayList<>();
    private BottomSheetBinding binding;
    public static final String TAG = "BottomSheetFragment";
    MainViewModel mMainViewModel;

    public DialogBookFragment() {

    }

    public DialogBookFragment(ReaderFragment.TOCAdapter TOCAdapter,
            List<Storage.Bookmark> bookmarkList, FBReaderView fbReaderView) {
        mTOCAdapter = TOCAdapter;
        mBookmarkList = bookmarkList;
        mFBReaderView = fbReaderView;
    }

    public DialogBookFragment(TOCTree TOCTree, List<TOCTree> TOCTreeList,
            ReaderFragment.TOCAdapter TOCAdapter) {
        mTOCTree = TOCTree;
        mTOCTreeList = TOCTreeList;
        mTOCAdapter = TOCAdapter;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.FullScreenDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet, container, false);
        return binding.getRoot();//super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        setUpAdapter();
        observerData();
    }

    private void setUpAdapter() {
        ViewPagerAdapters adapter =
                new ViewPagerAdapters(requireActivity(), mTOCAdapter, mBookmarkList, mFBReaderView);
        binding.viewPager.setAdapter(adapter);
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Mục lục");
            } else {
                tab.setText("Đánh dấu & ghi chú");
            }
        }).attach();

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mMainViewModel.eventLoadFrNote.setValue(true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void observerData() {
        mMainViewModel.eventCloseBookMark.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean && isAdded() && isVisible()) {
                    mMainViewModel.eventCloseBookMark.setValue(false);
                    dismissAllowingStateLoss();
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int widthAndHeight = ViewGroup.LayoutParams.MATCH_PARENT;
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(widthAndHeight, widthAndHeight);
            }
        }
    }
}
