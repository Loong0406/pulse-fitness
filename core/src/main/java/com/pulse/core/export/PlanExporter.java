package com.pulse.core.export;

import com.pulse.core.model.*;
import com.pulse.core.plan.*;
import com.pulse.core.util.JsonCodec;
import java.io.IOException;

/** Bridge: export format and output destination vary independently. */
public abstract class PlanExporter {
    public interface Sink {
        void write(String text) throws IOException;
    }

    protected final Sink sink;

    protected PlanExporter(Sink sink) {
        this.sink = sink;
    }

    public abstract void export(WeekPlan plan) throws IOException;

    public static final class Json extends PlanExporter {
        public Json(Sink sink) {
            super(sink);
        }

        public void export(WeekPlan p) throws IOException {
            sink.write(JsonCodec.plan(p));
        }
    }

    public static final class Text extends PlanExporter {
        public Text(Sink sink) {
            super(sink);
        }

        public void export(WeekPlan p) throws IOException {
            StringBuilder b =
                    new StringBuilder("脉动 PULSE · 一周训练计划\n来源：")
                            .append(p.source == WeekPlan.Source.AI ? "大模型" : "本地规则")
                            .append("\n")
                            .append(p.note)
                            .append("\n");
            for (DayPlan d : p) {
                b.append("\n周")
                        .append("一二三四五六日".charAt(d.dayIndex))
                        .append(" · ")
                        .append(d.title)
                        .append("\n");
                for (Prescription rx : d.exercises) {
                    Exercise e = ExerciseCatalog.get(rx.exerciseId);
                    b.append(e.name)
                            .append(" ")
                            .append(rx.sets)
                            .append("组 × ")
                            .append(rx.amount)
                            .append(e.timed ? "秒" : "次")
                            .append("，休息")
                            .append(rx.restSeconds)
                            .append("秒\n");
                }
            }
            sink.write(b.toString());
        }
    }

    public static final class MemorySink implements Sink {
        public String value;

        public void write(String text) {
            value = text;
        }
    }
}
