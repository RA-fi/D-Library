package com.example.dlibrary;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class NewspaperActivity extends AppCompatActivity {

    private TextInputEditText inputSearch;
    private ChipGroup chipGroupPapers;
    private RecyclerView recyclerView;

    private final List<NewspaperModel> allPapers = new ArrayList<>();
    private final List<NewspaperModel> displayedPapers = new ArrayList<>();
    private NewspaperAdapter adapter;

    private String selectedCategoryFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newspaper);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📰 Digital E-Newspapers");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        inputSearch = findViewById(R.id.input_paper_search);
        chipGroupPapers = findViewById(R.id.chip_group_papers);
        recyclerView = findViewById(R.id.recycler_newspapers);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        initNewspapers();
        setupListeners();
        filterNewspapers();
    }

    private void initNewspapers() {
        allPapers.clear();
        // National Dailies (Bangladesh)
        allPapers.add(new NewspaperModel("Prothom Alo", "Bengali • National Daily", "https://epaper.prothomalo.com", "National"));
        allPapers.add(new NewspaperModel("The Daily Star", "English • National Daily", "https://epaper.thedailystar.net", "National"));
        allPapers.add(new NewspaperModel("Daily Ittefaq", "Bengali • National Daily", "https://epaper.ittefaq.com.bd", "National"));
        allPapers.add(new NewspaperModel("Dhaka Tribune", "English • National Daily", "https://epaper.dhakatribune.com", "National"));
        allPapers.add(new NewspaperModel("Daily Samakal", "Bengali • National Daily", "https://epaper.samakal.com", "National"));
        allPapers.add(new NewspaperModel("The Financial Express", "English • Financial Daily", "https://epaper.thefinancialexpress.com.bd", "National"));

        // International Dailies & News Networks
        allPapers.add(new NewspaperModel("BBC News", "English • Global News Portal", "https://www.bbc.com/news", "International"));
        allPapers.add(new NewspaperModel("CNN International", "English • World News Network", "https://edition.cnn.com", "International"));
        allPapers.add(new NewspaperModel("The New York Times", "English • International Newspaper", "https://www.nytimes.com", "International"));
        allPapers.add(new NewspaperModel("The Guardian", "English • Global Edition", "https://www.theguardian.com/international", "International"));
        allPapers.add(new NewspaperModel("Reuters", "English • Financial & World News", "https://www.reuters.com", "International"));
        allPapers.add(new NewspaperModel("Al Jazeera", "English • Middle East & Global News", "https://www.aljazeera.com", "International"));
    }

    private void setupListeners() {
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNewspapers();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupPapers.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_paper_national) {
                selectedCategoryFilter = "National";
            } else if (checkedId == R.id.chip_paper_international) {
                selectedCategoryFilter = "International";
            } else {
                selectedCategoryFilter = "All";
            }
            filterNewspapers();
        });
    }

    private void filterNewspapers() {
        String query = inputSearch.getText() != null ? inputSearch.getText().toString().trim().toLowerCase() : "";

        displayedPapers.clear();
        for (NewspaperModel paper : allPapers) {
            boolean matchesCategory = "All".equalsIgnoreCase(selectedCategoryFilter) || paper.category.equalsIgnoreCase(selectedCategoryFilter);
            boolean matchesQuery = query.isEmpty() || paper.name.toLowerCase().contains(query) || paper.desc.toLowerCase().contains(query);

            if (matchesCategory && matchesQuery) {
                displayedPapers.add(paper);
            }
        }

        if (adapter == null) {
            adapter = new NewspaperAdapter(displayedPapers);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private static class NewspaperModel {
        String name, desc, url, category;

        NewspaperModel(String name, String desc, String url, String category) {
            this.name = name;
            this.desc = desc;
            this.url = url;
            this.category = category;
        }
    }

    private class NewspaperAdapter extends RecyclerView.Adapter<NewspaperAdapter.PaperViewHolder> {

        private final List<NewspaperModel> list;

        NewspaperAdapter(List<NewspaperModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public PaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_newspaper, parent, false);
            return new PaperViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PaperViewHolder holder, int position) {
            NewspaperModel paper = list.get(position);
            holder.txtName.setText(paper.name);
            holder.txtLang.setText(paper.desc);

            holder.btnRead.setOnClickListener(v -> {
                Intent intent = new Intent(NewspaperActivity.this, NewspaperWebViewActivity.class);
                intent.putExtra("paper_url", paper.url);
                intent.putExtra("paper_name", paper.name);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class PaperViewHolder extends RecyclerView.ViewHolder {
            TextView txtName, txtLang;
            MaterialButton btnRead;

            PaperViewHolder(@NonNull View itemView) {
                super(itemView);
                txtName = itemView.findViewById(R.id.txt_paper_name);
                txtLang = itemView.findViewById(R.id.txt_paper_lang);
                btnRead = itemView.findViewById(R.id.btn_read_paper);
            }
        }
    }
}
