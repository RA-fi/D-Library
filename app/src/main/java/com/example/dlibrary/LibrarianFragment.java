package com.example.dlibrary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LibrarianFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_librarian, container, false);

        TextInputEditText inputSubject = view.findViewById(R.id.input_query_subject);
        TextInputEditText inputMessage = view.findViewById(R.id.input_query_message);
        MaterialButton btnSend = view.findViewById(R.id.btn_send_query);

        btnSend.setOnClickListener(v -> {
            String subject = inputSubject.getText() != null ? inputSubject.getText().toString().trim() : "";
            String message = inputMessage.getText() != null ? inputMessage.getText().toString().trim() : "";

            if (subject.isEmpty() || message.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill subject and message", Toast.LENGTH_SHORT).show();
                return;
            }

            inputSubject.setText("");
            inputMessage.setText("");
            Toast.makeText(requireContext(), "Your query has been sent to D Library staff. We will contact you soon!", Toast.LENGTH_LONG).show();
        });

        return view;
    }
}
