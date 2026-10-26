package com.pulse.core.plan;

import com.pulse.core.model.Exercise;
import com.pulse.core.model.UserProfile;
import java.util.*;
import static com.pulse.core.model.UserProfile.Equipment.*;

/** Shared immutable exercise definitions (Flyweight). IDs are also the model's action whitelist. */
public final class ExerciseCatalog {
    private static final Map<String, Exercise> ITEMS;

    static {
        Map<String, Exercise> m = new LinkedHashMap<>();
        add(
                m,
                new Exercise(
                        "wall_push",
                        "靠墙俯卧撑",
                        "上肢",
                        BODYWEIGHT,
                        false,
                        false,
                        false,
                        false,
                        "身体保持直线，缓慢靠近墙面。",
                        "双手撑墙，与肩同宽。",
                        "屈肘让胸口靠近墙面。",
                        "呼气推回，避免耸肩。"));
        add(
                m,
                new Exercise(
                        "pushup",
                        "跪姿俯卧撑",
                        "上肢",
                        BODYWEIGHT,
                        false,
                        false,
                        true,
                        false,
                        "收紧腹部，肘部自然向后。",
                        "双膝垫软垫，双手撑地。",
                        "缓慢屈肘降低身体。",
                        "推起时躯干保持稳定。"));
        add(
                m,
                new Exercise(
                        "squat",
                        "自重深蹲",
                        "下肢",
                        BODYWEIGHT,
                        false,
                        true,
                        true,
                        false,
                        "在无痛范围内下蹲，不追求深度。",
                        "双脚与肩同宽。",
                        "髋部向后坐，膝盖沿脚尖方向。",
                        "脚掌稳定发力站起。"));
        add(
                m,
                new Exercise(
                        "chair_squat",
                        "椅子坐站",
                        "下肢",
                        BODYWEIGHT,
                        false,
                        true,
                        false,
                        false,
                        "使用靠墙的稳固椅子。",
                        "坐在固定椅子前缘。",
                        "双脚踩稳，身体微向前。",
                        "缓慢站起再坐下。"));
        add(
                m,
                new Exercise(
                        "calf_raise",
                        "站姿提踵",
                        "下肢",
                        BODYWEIGHT,
                        false,
                        false,
                        false,
                        false,
                        "扶稳墙面，脚跟缓慢升降。",
                        "双脚平行站立。",
                        "扶墙保持平衡，抬起脚跟。",
                        "缓慢落回，不弹震。"));
        add(
                m,
                new Exercise(
                        "side_leg",
                        "站姿侧抬腿",
                        "下肢",
                        BODYWEIGHT,
                        false,
                        false,
                        false,
                        false,
                        "扶稳支撑物，左右交替完成总次数。",
                        "侧身扶墙站稳。",
                        "一侧腿缓慢向外抬。",
                        "保持骨盆稳定，交替两侧。"));
        add(
                m,
                new Exercise(
                        "bridge",
                        "臀桥",
                        "下肢",
                        BODYWEIGHT,
                        false,
                        false,
                        true,
                        false,
                        "抬起到躯干与大腿接近直线。",
                        "屈膝仰卧，双脚踩地。",
                        "收紧臀部，缓慢抬髋。",
                        "避免腰部过度后伸。"));
        add(
                m,
                new Exercise(
                        "dead_bug",
                        "死虫式",
                        "核心",
                        BODYWEIGHT,
                        false,
                        false,
                        true,
                        false,
                        "腰部自然稳定，左右交替完成总次数。",
                        "仰卧，抬起双臂双腿。",
                        "对侧手脚缓慢伸展。",
                        "回到起点，再换另一侧。"));
        add(
                m,
                new Exercise(
                        "bird_dog",
                        "鸟狗式",
                        "核心",
                        BODYWEIGHT,
                        false,
                        true,
                        true,
                        false,
                        "保持躯干稳定，左右交替。",
                        "四点支撑，膝盖下垫软垫。",
                        "伸展对侧手脚。",
                        "停顿后收回，换边。"));
        add(
                m,
                new Exercise(
                        "standing_core",
                        "站姿交替抬膝",
                        "核心",
                        BODYWEIGHT,
                        false,
                        false,
                        false,
                        false,
                        "小幅度抬膝，保持自然呼吸。",
                        "扶墙站稳。",
                        "缓慢交替抬膝。",
                        "保持躯干竖直，不憋气。"));
        add(
                m,
                new Exercise(
                        "march",
                        "原地轻踏步",
                        "有氧",
                        BODYWEIGHT,
                        true,
                        false,
                        false,
                        false,
                        "轻落地，以能正常说话的强度进行。",
                        "身体直立，双脚交替踏步。",
                        "双臂自然摆动。",
                        "保持舒适呼吸，必要时减速。"));
        add(
                m,
                new Exercise(
                        "step_touch",
                        "左右侧点步",
                        "有氧",
                        BODYWEIGHT,
                        true,
                        false,
                        false,
                        false,
                        "小步侧移，膝关节自然放松。",
                        "向一侧轻迈一步。",
                        "另一脚跟近轻点地面。",
                        "左右交替，保持低冲击。"));
        add(
                m,
                new Exercise(
                        "jumping_jack",
                        "开合跳",
                        "有氧",
                        BODYWEIGHT,
                        true,
                        true,
                        true,
                        true,
                        "轻柔落地，膝盖不适时停止。",
                        "站立准备。",
                        "轻跳向外打开双脚并抬臂。",
                        "跳回起点，控制节奏。"));
        add(
                m,
                new Exercise(
                        "arm_circle",
                        "站姿肩部绕环",
                        "活动度",
                        BODYWEIGHT,
                        true,
                        false,
                        false,
                        false,
                        "小幅度、慢速绕环，避免疼痛范围。",
                        "自然站立。",
                        "双臂向两侧打开到舒适位置。",
                        "缓慢绕小圈，再换方向。"));
        add(
                m,
                new Exercise(
                        "chest_open",
                        "站姿开胸",
                        "活动度",
                        BODYWEIGHT,
                        true,
                        false,
                        false,
                        false,
                        "保持肩部放松，自然呼吸。",
                        "站直，双手放在身体两侧。",
                        "手臂轻向后打开。",
                        "在舒适幅度保持，不用力拉伸。"));
        add(
                m,
                new Exercise(
                        "ankle",
                        "坐姿踝泵",
                        "活动度",
                        BODYWEIGHT,
                        true,
                        false,
                        false,
                        false,
                        "坐稳后缓慢活动踝关节。",
                        "坐在稳固椅子上。",
                        "脚尖缓慢勾起，再向下点。",
                        "两侧交替，自然呼吸。"));
        add(
                m,
                new Exercise(
                        "db_row",
                        "支撑哑铃划船",
                        "上肢",
                        DUMBBELL,
                        false,
                        false,
                        true,
                        false,
                        "选用能轻松控制的重量，左右交替。",
                        "一手扶稳固支撑。",
                        "另一手持哑铃向髋部提拉。",
                        "缓慢还原，避免扭转腰部。"));
        add(
                m,
                new Exercise(
                        "db_curl",
                        "站姿哑铃弯举",
                        "上肢",
                        DUMBBELL,
                        false,
                        false,
                        false,
                        false,
                        "肘部贴近身体，不借力甩动。",
                        "双手持轻哑铃自然站立。",
                        "屈肘向上提起。",
                        "缓慢放下，保持手腕中立。"));
        add(
                m,
                new Exercise(
                        "db_press",
                        "坐姿哑铃推举",
                        "上肢",
                        DUMBBELL,
                        false,
                        false,
                        true,
                        false,
                        "使用稳固有靠背的座椅和轻重量。",
                        "坐稳，哑铃置于肩部两侧。",
                        "在舒适范围向上推起。",
                        "缓慢回落，避免憋气。"));
        add(
                m,
                new Exercise(
                        "lat_pull",
                        "高位下拉",
                        "上肢",
                        GYM,
                        false,
                        false,
                        true,
                        false,
                        "动作缓慢，避免向后猛仰。",
                        "调整座位，握住下拉杆。",
                        "向胸前方向平稳下拉。",
                        "控制还原，保持肩部放松。"));
        add(
                m,
                new Exercise(
                        "cable_row",
                        "坐姿器械划船",
                        "上肢",
                        GYM,
                        false,
                        false,
                        true,
                        false,
                        "保持自然脊柱位置。",
                        "调整器械并坐稳。",
                        "握把向腹部平稳拉回。",
                        "缓慢还原，不耸肩。"));
        add(
                m,
                new Exercise(
                        "leg_curl",
                        "坐姿腿弯举",
                        "下肢",
                        GYM,
                        false,
                        true,
                        false,
                        false,
                        "按器械说明调整，使用轻重量。",
                        "调整座位与挡垫。",
                        "屈膝缓慢带动器械。",
                        "控制还原，不锁死关节。"));
        ITEMS = Collections.unmodifiableMap(m);
    }

    private static void add(Map<String, Exercise> m, Exercise e) {
        m.put(e.id, e);
    }

    public static Exercise get(String id) {
        Exercise e = ITEMS.get(id);
        if (e == null) throw new IllegalArgumentException("未知动作：" + id);
        return e;
    }

    public static List<Exercise> all() {
        return Collections.unmodifiableList(new ArrayList<>(ITEMS.values()));
    }

    public static List<Exercise> allowed(UserProfile p) {
        List<Exercise> r = new ArrayList<>();
        for (Exercise e : ITEMS.values()) if (e.isAllowed(p)) r.add(e);
        return r;
    }

    private ExerciseCatalog() {}
}
