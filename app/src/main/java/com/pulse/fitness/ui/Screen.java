package com.pulse.fitness.ui;

import android.view.View;
import com.pulse.fitness.presentation.FitnessViewModel;

public interface Screen {
    View view();

    void bind(FitnessViewModel.UiState state);
}
