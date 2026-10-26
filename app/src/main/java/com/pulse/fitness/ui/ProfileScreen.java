package com.pulse.fitness.ui;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.widget.*;
import com.pulse.fitness.*;
import com.pulse.fitness.presentation.FitnessViewModel;
import com.pulse.core.model.UserProfile;
import java.util.Locale;

public final class ProfileScreen implements Screen {
    private final View root;
    private final Context context;
    private final FitnessViewModel vm;
    private final int[] minuteOptions = {15, 20, 30, 40, 45, 60};

    public ProfileScreen(Context c, FitnessViewModel vm, NavigationMediator navigation) {
        context = c;
        this.vm = vm;
        root = Ui.screen(c, R.layout.screen_profile);
        options(R.id.profile_sex, new String[] {"不透露", "女", "男"});
        options(R.id.profile_goal, new String[] {"减脂塑形", "增肌力量", "改善体能"});
        options(R.id.profile_experience, new String[] {"刚开始训练", "已有规律训练", "有长期训练经验"});
        options(R.id.profile_equipment, new String[] {"徒手 / 居家", "哑铃 + 徒手", "健身房 / 综合器械"});
        options(R.id.profile_days, new String[] {"每周 2 天", "每周 3 天", "每周 4 天", "每周 5 天"});
        options(
                R.id.profile_minutes,
                new String[] {"15 分钟", "20 分钟", "30 分钟", "40 分钟", "45 分钟", "60 分钟"});
        populate(vm.state.get().profile);
        ((EditText) root.findViewById(R.id.profile_endpoint))
                .setText(AppContainer.get().endpoint());
        ((EditText) root.findViewById(R.id.profile_token)).setText(AppContainer.get().tokens.get());
        root.findViewById(R.id.profile_save)
                .setOnClickListener(
                        v -> {
                            try {
                                UserProfile p =
                                        new UserProfile.Builder()
                                                .name(value(R.id.profile_name))
                                                .age(Integer.parseInt(value(R.id.profile_age)))
                                                .sex(
                                                        (String)
                                                                spinner(R.id.profile_sex)
                                                                        .getSelectedItem())
                                                .body(
                                                        Double.parseDouble(
                                                                value(R.id.profile_height)),
                                                        Double.parseDouble(
                                                                value(R.id.profile_weight)))
                                                .goal(
                                                        UserProfile.Goal.values()[
                                                                position(R.id.profile_goal)])
                                                .experience(
                                                        UserProfile.Experience.values()[
                                                                position(R.id.profile_experience)])
                                                .equipment(
                                                        UserProfile.Equipment.values()[
                                                                position(R.id.profile_equipment)])
                                                .schedule(
                                                        position(R.id.profile_days) + 2,
                                                        minuteOptions[
                                                                position(R.id.profile_minutes)])
                                                .limitations(
                                                        checked(R.id.profile_knees),
                                                        checked(R.id.profile_back),
                                                        checked(R.id.profile_low_impact),
                                                        checked(R.id.profile_professional))
                                                .build();
                                vm.saveProfile(p);
                                Ui.toast(c, "档案已保存");
                                navigation.show(NavigationMediator.Tab.PLAN);
                            } catch (NumberFormatException e) {
                                Ui.text(root, R.id.profile_error, "请输入有效的年龄、身高和体重");
                            } catch (IllegalArgumentException e) {
                                Ui.text(root, R.id.profile_error, e.getMessage());
                            }
                        });
        root.findViewById(R.id.profile_undo)
                .setOnClickListener(
                        v -> {
                            vm.undo();
                            populate(vm.state.get().profile);
                            Ui.toast(c, "已撤销档案修改");
                        });
        root.findViewById(R.id.profile_connect)
                .setOnClickListener(
                        v ->
                                navigation.runWithBackendAccess(
                                        () ->
                                                vm.testConnection(
                                                        value(R.id.profile_endpoint),
                                                        value(R.id.profile_token))));
        root.findViewById(R.id.profile_account_action)
                .setOnClickListener(
                        v -> {
                            if (!AppContainer.get().sessions.isPhoneSession()) {
                                c.startActivity(
                                        new Intent(c, LoginActivity.class)
                                                .putExtra(LoginActivity.FORCE_LOGIN, true));
                                return;
                            }
                            new AlertDialog.Builder(c)
                                    .setTitle(R.string.auth_sign_out_title)
                                    .setMessage(R.string.auth_sign_out_message)
                                    .setNegativeButton(R.string.auth_cancel, null)
                                    .setPositiveButton(
                                            R.string.auth_account_sign_out,
                                            (d, w) -> {
                                                AppContainer.get().sessions.clear();
                                                AppContainer.get().phoneAuth.clear();
                                                LoginActivity.openFresh((Activity) c);
                                            })
                                    .show();
                        });
        root.findViewById(R.id.profile_delete)
                .setOnClickListener(
                        v ->
                                new AlertDialog.Builder(c)
                                        .setTitle("清除本机个人数据？")
                                        .setMessage(
                                                "这会删除本机登录状态、档案、计划、训练记录、收藏、训练进度和服务连接信息，无法恢复。不会删除服务端数据库中的演示用户和登录记录。")
                                        .setNegativeButton("取消", null)
                                        .setPositiveButton(
                                                "清除数据",
                                                (dialog, which) -> {
                                                    vm.reset();
                                                    LoginActivity.openFresh((Activity) c);
                                                    Ui.toast(c, "本机数据已清除");
                                                })
                                        .show());
    }

    public View view() {
        return root;
    }

    public void bind(FitnessViewModel.UiState s) {
        root.findViewById(R.id.profile_undo).setEnabled(vm.canUndo());
        Ui.text(root, R.id.profile_connection_status, s.connection);
        boolean phone = AppContainer.get().sessions.isPhoneSession();
        boolean sql = AppContainer.get().sessions.isDatabaseSession();
        Ui.text(
                root,
                R.id.profile_account_title,
                context.getString(
                        sql
                                ? R.string.auth_sql_account
                                : phone
                                        ? R.string.auth_account_demo
                                        : R.string.auth_account_guest));
        Ui.text(
                root,
                R.id.profile_account_note,
                phone
                        ? context.getString(
                                sql
                                        ? R.string.auth_sql_account_note
                                        : R.string.auth_account_phone_note,
                                AppContainer.get().sessions.maskedPhone())
                        : context.getString(R.string.auth_account_guest_note));
        Ui.text(
                root,
                R.id.profile_account_action,
                context.getString(
                        phone ? R.string.auth_account_sign_out : R.string.auth_account_sign_in));
    }

    private void populate(UserProfile raw) {
        UserProfile p = raw == null ? UserProfile.defaults() : raw;
        edit(R.id.profile_name, p.name);
        edit(R.id.profile_age, String.valueOf(p.age));
        edit(R.id.profile_height, number(p.heightCm));
        edit(R.id.profile_weight, number(p.weightKg));
        spinner(R.id.profile_sex).setSelection(p.sex.equals("女") ? 1 : p.sex.equals("男") ? 2 : 0);
        spinner(R.id.profile_goal).setSelection(p.goal.ordinal());
        spinner(R.id.profile_experience).setSelection(p.experience.ordinal());
        spinner(R.id.profile_equipment).setSelection(p.equipment.ordinal());
        spinner(R.id.profile_days).setSelection(p.daysPerWeek - 2);
        int minute = 0;
        for (int i = 0; i < minuteOptions.length; i++)
            if (minuteOptions[i] == p.minutesPerSession) minute = i;
        spinner(R.id.profile_minutes).setSelection(minute);
        check(R.id.profile_knees, p.protectKnees);
        check(R.id.profile_back, p.protectBack);
        check(R.id.profile_low_impact, p.lowImpact);
        check(R.id.profile_professional, p.needsProfessional);
    }

    private String number(double value) {
        return value == (int) value
                ? String.valueOf((int) value)
                : String.format(Locale.ROOT, "%.1f", value);
    }

    private void edit(int id, String s) {
        ((EditText) root.findViewById(id)).setText(s);
    }

    private void check(int id, boolean value) {
        ((CheckBox) root.findViewById(id)).setChecked(value);
    }

    private String value(int id) {
        return ((EditText) root.findViewById(id)).getText().toString().trim();
    }

    private boolean checked(int id) {
        return ((CheckBox) root.findViewById(id)).isChecked();
    }

    private Spinner spinner(int id) {
        return root.findViewById(id);
    }

    private int position(int id) {
        return spinner(id).getSelectedItemPosition();
    }

    private void options(int id, String[] labels) {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, labels);
        spinner(id).setAdapter(adapter);
    }
}
