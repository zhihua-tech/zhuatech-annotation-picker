package cn.zhuatech.annotationpicker;

import java.util.*;

/**
 * 在同一熵排序和稳定并列规则下，分别试算“总预算加一”和“单来源上限加一”，给出新增、移出编号与覆盖来源变化。参数最高 200；试算不改原入选清单。 上海如静知华信息科技有限公司 ·
 * https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
final class Diagnostics {
  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  private static Map<String, Object> map(Object... pairs) {
    var m = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
    return m;
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  private static double num(Map<String, Object> m, String k) {
    return ((Number) m.get(k)).doubleValue();
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  private static Double ratio(double a, double b) {
    return b == 0 ? null : a / b;
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @SuppressWarnings("unchecked")
  static Map<String, Object> evaluate(
      Map<String, Object> cfg, List<Map<String, Object>> rows, Map<String, Object> base) {
    var metrics = (Map<String, Object>) base.get("metrics");
    var evidence = (List<Map<String, Object>>) base.get("rows");
    var details = new ArrayList<Map<String, Object>>();
    int budget = (int) num(cfg, "budget"), cap = (int) num(cfg, "groupCap");
    var initial = new LinkedHashSet<String>();
    for (var r : evidence) if ("入选".equals(r.get("reason"))) initial.add((String) r.get("id"));
    int gain = 0, capChanges = 0;
    for (int i = 0; i < 2; i++) {
      int b = i == 0 ? Math.min(200, budget + 1) : budget,
          k = i == 1 ? Math.min(200, cap + 1) : cap;
      var used = new HashMap<String, Integer>();
      var picked = new LinkedHashSet<String>();
      for (var r : evidence) {
        String group = (String) r.get("group");
        if (picked.size() >= b) break;
        if (used.getOrDefault(group, 0) >= k) continue;
        picked.add((String) r.get("id"));
        used.merge(group, 1, Integer::sum);
      }
      var added = new LinkedHashSet<>(picked);
      added.removeAll(initial);
      var lost = new LinkedHashSet<>(initial);
      lost.removeAll(picked);
      if (i == 0) gain = picked.size() - initial.size();
      else capChanges = added.size();
      details.add(
          map(
              "scenario",
              i == 0 ? "预算加一" : "来源上限加一",
              "budgetUsed",
              b,
              "groupCapUsed",
              k,
              "gainedIds",
              List.copyOf(added),
              "lostIds",
              List.copyOf(lost),
              "sourceCount",
              used.size(),
              "selectedCount",
              picked.size()));
    }
    return Checks.result(map("maxBudgetGain", gain, "capChangedSelection", capChanges), details);
  }
}
