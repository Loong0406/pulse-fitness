package com.pulse.core.plan;

import com.pulse.core.model.Exercise;
import java.util.Locale;

/** Interpreter grammar: or := and ('|' and)*; and := term ('&' term)*;
 * term := free-text | group:value | equipment:value. No executable expressions. */
public final class ExerciseQuery {
    public interface Expression {
        boolean matches(Exercise e);
    }

    private static final class Term implements Expression {
        private final String key, value;

        Term(String text) {
            String[] pair = text.trim().split(":", 2);
            key = pair.length == 2 ? pair[0].trim() : "text";
            value = pair[pair.length - 1].trim().toLowerCase(Locale.ROOT);
        }

        public boolean matches(Exercise e) {
            switch (key) {
                case "group":
                    return e.group.contains(value);
                case "equipment":
                    return e.equipmentLabel.contains(value);
                default:
                    return (e.name + e.group + e.equipmentLabel)
                            .toLowerCase(Locale.ROOT)
                            .contains(value);
            }
        }
    }

    private static final class And implements Expression {
        private final Expression left, right;

        And(Expression l, Expression r) {
            left = l;
            right = r;
        }

        public boolean matches(Exercise e) {
            return left.matches(e) && right.matches(e);
        }
    }

    private static final class Or implements Expression {
        private final Expression left, right;

        Or(Expression l, Expression r) {
            left = l;
            right = r;
        }

        public boolean matches(Exercise e) {
            return left.matches(e) || right.matches(e);
        }
    }

    public static Expression parse(String text) {
        if (text == null || text.trim().isEmpty()) return e -> true;
        if (text.length() > 160) throw new IllegalArgumentException("搜索内容最多 160 字");
        Expression result = null;
        for (String clause : text.split("\\|", -1)) {
            Expression branch = null;
            for (String token : clause.split("&", -1)) {
                if (token.trim().isEmpty()) throw new IllegalArgumentException("筛选条件不完整");
                Expression term = new Term(token);
                branch = branch == null ? term : new And(branch, term);
            }
            result = result == null ? branch : new Or(result, branch);
        }
        return result;
    }

    private ExerciseQuery() {}
}
