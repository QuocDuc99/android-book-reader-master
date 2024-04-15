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
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.databinding.FragmentTableOfContentBinding;

public class FragmentTableOfContent extends Fragment {
    private TOCTree mTOCTree;
    private List<TOCTree> mTOCTreeList;
    private ReaderFragment.TOCAdapter mTOCAdapter;


    public FragmentTableOfContent() {

    }

    public FragmentTableOfContent(ReaderFragment.TOCAdapter TOCAdapter) {
        mTOCAdapter = TOCAdapter;
    }

    private FragmentTableOfContentBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_table_of_content, container,
                false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new Handler().postDelayed(() -> {
            setUpAdapter();
            binding.prLoading.setVisibility(View.INVISIBLE);
        }, 300);

    }

    private void setUpAdapter() {
        binding.rcMucLuc.setAdapter((RecyclerView.Adapter) mTOCAdapter);
    }
}
