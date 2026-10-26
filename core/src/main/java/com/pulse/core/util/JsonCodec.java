package com.pulse.core.util;

import com.google.gson.*;
import com.pulse.core.model.*;
import com.pulse.core.validation.ProfileValidator;
import java.util.*;

/** Explicit boundary mapping: constructors preserve immutable collection invariants. */
public final class JsonCodec {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public static String profile(UserProfile p) {
        return GSON.toJson(p);
    }

    public static UserProfile profile(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        UserProfile p =
                new UserProfile.Builder()
                        .name(str(o, "name"))
                        .age(integer(o, "age"))
                        .sex(str(o, "sex"))
                        .body(o.get("heightCm").getAsDouble(), o.get("weightKg").getAsDouble())
                        .goal(UserProfile.Goal.valueOf(str(o, "goal")))
                        .experience(UserProfile.Experience.valueOf(str(o, "experience")))
                        .equipment(UserProfile.Equipment.valueOf(str(o, "equipment")))
                        .schedule(integer(o, "daysPerWeek"), integer(o, "minutesPerSession"))
                        .limitations(
                                bool(o, "protectKnees"),
                                bool(o, "protectBack"),
                                bool(o, "lowImpact"),
                                bool(o, "needsProfessional"))
                        .build();
        ProfileValidator.validateForStorage(p);
        return p;
    }

    public static String plan(WeekPlan p) {
        return GSON.toJson(p);
    }

    public static WeekPlan plan(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        return new WeekPlan(
                str(o, "id"),
                o.get("createdAt").getAsLong(),
                str(o, "profileSignature"),
                WeekPlan.Source.valueOf(str(o, "source")),
                str(o, "note"),
                days(o.getAsJsonArray("days")));
    }

    public static List<DayPlan> days(JsonArray array) {
        if (array == null || array.size() != 7) throw new IllegalArgumentException("计划必须包含七天");
        List<DayPlan> days = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject d = element.getAsJsonObject();
            List<Prescription> exercises = new ArrayList<>();
            JsonArray rx = d.getAsJsonArray("exercises");
            if (rx == null || rx.size() > 6) throw new IllegalArgumentException("动作数量无效");
            for (JsonElement r : rx) {
                JsonObject x = r.getAsJsonObject();
                exercises.add(
                        new Prescription(
                                str(x, "exerciseId"),
                                integer(x, "sets"),
                                integer(x, "amount"),
                                integer(x, "restSeconds")));
            }
            days.add(new DayPlan(integer(d, "dayIndex"), str(d, "title"), exercises));
        }
        return days;
    }

    public static String str(JsonObject o, String key) {
        JsonElement v = o.get(key);
        if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("缺少文本字段：" + key);
        return v.getAsString();
    }

    public static int integer(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("缺少数字字段：" + key);
        try {
            return e.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("数字必须为整数：" + key);
        }
    }

    private static boolean bool(JsonObject o, String key) {
        JsonElement v = o.get(key);
        if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("缺少开关字段：" + key);
        return v.getAsBoolean();
    }

    private JsonCodec() {}
}
