package com.github.axet.bookreader.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.github.axet.bookreader.R;

public class MainActivity extends AppCompatActivity {
    private TextView openBook;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_main1);
        String pathBook = "https://cdn-file.thuvien.edu.vn/Uploads/THU_VIEN/shn/3/1823/UserFiles/TV5-2-5d70c0b3-08c8-4b60-bc8e-6d4dbeeb7c60.pdf";
        openBook = findViewById(R.id.clickButton);
        openBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ducNQ", "onClickss: ");
                startActivity(BookActivity.newInstance(MainActivity.this,pathBook));
            }
        });
    }
}
