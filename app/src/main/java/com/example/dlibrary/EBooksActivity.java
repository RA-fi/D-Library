package com.example.dlibrary;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class EBooksActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, ResourceListActivity.class);
        intent.putExtra("list_type", "TYPE_FILTER");
        intent.putExtra("filter_value", "Book");
        intent.putExtra("title", "Digital Books & E-Books");
        startActivity(intent);
        finish();
    }
}
