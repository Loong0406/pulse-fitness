package com.pulse.fitness.ui;

import android.content.Context;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.pulse.fitness.*;
import com.pulse.fitness.presentation.FitnessViewModel;
import com.pulse.core.model.Exercise;
import com.pulse.core.plan.*;
import java.util.Set;

public final class ExercisesScreen implements Screen {
    private final View root;
    private final Context context;
    private String filter = "全部";

    public ExercisesScreen(Context c) {
        context = c;
        root = Ui.screen(c, R.layout.screen_exercises);
        LinearLayout filters = root.findViewById(R.id.exercise_filters);
        for (String label : new String[] {"全部", "上肢", "下肢", "核心", "有氧", "活动度", "收藏"}) {
            TextView chip = new TextView(c);
            chip.setText(label);
            chip.setTextColor(c.getColorStateList(R.color.chip_text));
            chip.setTextSize(13);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(Ui.dp(c, 16), 0, Ui.dp(c, 16), 0);
            chip.setBackgroundResource(R.drawable.bg_chip);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, Ui.dp(c, 48));
            lp.setMarginEnd(Ui.dp(c, 8));
            filters.addView(chip, lp);
            chip.setOnClickListener(
                    v -> {
                        filter = label;
                        for (int i = 0; i < filters.getChildCount(); i++) {
                            View item = filters.getChildAt(i);
                            item.setSelected(item == v);
                            item.setBackgroundResource(
                                    item == v ? R.drawable.bg_accent : R.drawable.bg_chip);
                        }
                        render();
                    });
        }
        filters.getChildAt(0).setBackgroundResource(R.drawable.bg_accent);
        filters.getChildAt(0).setSelected(true);
        ((EditText) root.findViewById(R.id.exercise_search))
                .addTextChangedListener(
                        new TextWatcher() {
                            public void beforeTextChanged(CharSequence s, int a, int n, int f) {}

                            public void onTextChanged(CharSequence s, int a, int b, int count) {
                                render();
                            }

                            public void afterTextChanged(Editable e) {}
                        });
        root.findViewById(R.id.exercise_count)
                .setOnLongClickListener(
                        v -> {
                            new android.app.AlertDialog.Builder(c)
                                    .setTitle("高级筛选")
                                    .setMessage(
                                            "支持简单条件表达式：\ngroup:上肢 & equipment:徒手\n\n使用 & 组合条件，使用 | 表示满足任一条件；也可以直接输入动作名。")
                                    .setPositiveButton("知道了", null)
                                    .show();
                            return true;
                        });
    }

    public View view() {
        return root;
    }

    public void bind(FitnessViewModel.UiState s) {
        render();
    }

    private void render() {
        LinearLayout list = root.findViewById(R.id.exercise_list);
        list.removeAllViews();
        String query = ((EditText) root.findViewById(R.id.exercise_search)).getText().toString();
        ExerciseQuery.Expression expression;
        try {
            expression = ExerciseQuery.parse(query);
        } catch (IllegalArgumentException e) {
            Ui.text(root, R.id.exercise_count, e.getMessage());
            return;
        }
        Set<String> favorites = AppContainer.get().favorites();
        int count = 0;
        for (Exercise e : ExerciseCatalog.all()) {
            if (!expression.matches(e)
                    || (filter.equals("收藏") && !favorites.contains(e.id))
                    || (!filter.equals("全部") && !filter.equals("收藏") && !e.group.equals(filter)))
                continue;
            count++;
            View row = LayoutInflater.from(context).inflate(R.layout.item_exercise, list, false);
            Ui.text(row, R.id.exercise_name, e.name);
            Ui.text(
                    row,
                    R.id.exercise_meta,
                    e.group + "  ·  " + e.equipmentLabel + "  ·  " + (e.timed ? "计时" : "计次"));
            Ui.text(
                    row,
                    R.id.exercise_symbol,
                    e.group.equals("核心")
                            ? "◎"
                            : e.group.equals("有氧") ? "↗" : e.group.equals("活动度") ? "≈" : "＋");
            TextView favorite = row.findViewById(R.id.exercise_favorite);
            favorite.setText(favorites.contains(e.id) ? "★" : "☆");
            favorite.setContentDescription((favorites.contains(e.id) ? "取消收藏" : "收藏") + e.name);
            favorite.setOnClickListener(
                    v -> {
                        AppContainer.get().favorite(e.id);
                        render();
                    });
            row.setOnClickListener(v -> Ui.detail(context, e));
            list.addView(row);
        }
        Ui.text(root, R.id.exercise_count, count + " 个动作  ·  点击查看要领，长按此处查看筛选语法");
        if (count == 0)
            list.addView(
                    Ui.paragraph(
                            context,
                            filter.equals("收藏") ? "还没有收藏。点击动作右侧的星标，留下常用动作。" : "没有找到匹配动作，试试其他关键词。"));
    }
}
