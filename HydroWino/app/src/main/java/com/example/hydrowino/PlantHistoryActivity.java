package com.example.hydrowino;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PlantHistoryActivity extends AppCompatActivity {

    private RecyclerView rvBatches;
    private android.view.View emptyStateLayout;
    private BatchAdapter adapter;
    private List<PlantBatch> batchList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_history);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvBatches = findViewById(R.id.rvBatches);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        rvBatches.setLayoutManager(new LinearLayoutManager(this));

        batchList = new ArrayList<>();
        adapter = new BatchAdapter(batchList);
        rvBatches.setAdapter(adapter);

        fetchHistoryData();
    }

    private void fetchHistoryData() {
        GreenhouseRepository.GreenhouseData data = GreenhouseRepository.getInstance().getCurrentGreenhouse();
        if (data == null || data.getGreenhouseID() == null) {
            showEmptyState(true);
            return;
        }
        String greenhouseId = data.getGreenhouseID();

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("PlantHistory")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    batchList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        PlantBatch batch = doc.toObject(PlantBatch.class);
                        if (batch != null) {
                            String batchGId = doc.getString("greenhouseId");
                            if (batchGId == null || batchGId.equals(greenhouseId)) {
                                batch.setId(doc.getId());
                                batchList.add(batch);
                            }
                        }
                    }
                    if (batchList.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    showEmptyState(true);
                });
    }

    private void showEmptyState(boolean show) {
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        if (rvBatches != null) {
            rvBatches.setVisibility(show ? android.view.View.GONE : android.view.View.VISIBLE);
        }
    }


}
