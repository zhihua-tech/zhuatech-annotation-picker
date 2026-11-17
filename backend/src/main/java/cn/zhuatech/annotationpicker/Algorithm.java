package cn.zhuatech.annotationpicker;

import static cn.zhuatech.annotationpicker.Checks.*;

import java.util.*;

/**
 * 计算多分类概率的香农熵，按不确定度挑选待标注样本，并按来源限制配额；输出每条入选或跳过的原因。 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
 * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
final class Algorithm {
  /**
   * 对已有模型概率抽样，不训练模型；全批须采用同一类别顺序和维度。熵以 bit 表示。 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
   * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
   */
  static Map<String, Object> compute(Map<String, Object> cfg, List<Map<String, Object>> rows) {
    var base = calculate(cfg, rows);
    var enriched = new LinkedHashMap<String, Object>(base);
    enriched.put("inspection", Inspection.evaluate(cfg, rows, base));
    enriched.put("diagnostics", Diagnostics.evaluate(cfg, rows, base));
    return enriched;
  }

  /**
   * 保留原始计算口径。上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
   */
  private static Map<String, Object> calculate(
      Map<String, Object> cfg, List<Map<String, Object>> rows) {
    must(rows != null && !rows.isEmpty() && rows.size() <= 200, "需要 1–200 行");
    unique(rows);
    var evidence = new ArrayList<Map<String, Object>>();
    int budget = integer(cfg, "budget", 1, 200), cap = integer(cfg, "groupCap", 1, 200), dim = -1;
    record Item(String id, String group, double entropy) {}
    var list = new ArrayList<Item>();
    for (var r : rows) {
      String g = str(r, "group", false);
      Object raw = r.get("probabilities");
      must(raw instanceof List<?>, "概率必须为数组");
      var a = (List<?>) raw;
      must(a.size() >= 2 && a.size() <= 512 && (dim == -1 || a.size() == dim), "类别维度不一致或超限");
      dim = a.size();
      double h = 0, sum = 0;
      for (var v : a) {
        must(v instanceof Number, "概率须为数值");
        double p = ((Number) v).doubleValue();
        must(Double.isFinite(p) && p >= 0 && p <= 1, "概率超界");
        sum += p;
        if (p > 0) h -= p * Math.log(p) / Math.log(2);
      }
      must(Math.abs(sum - 1) <= 1e-8, "概率之和应为 1");
      list.add(new Item((String) r.get("id"), g, h));
    }
    list.sort(Comparator.comparingDouble(Item::entropy).reversed().thenComparing(Item::id));
    var used = new HashMap<String, Integer>();
    int picked = 0;
    for (var v : list) {
      int n = used.getOrDefault(v.group(), 0);
      String why = n >= cap ? "来源配额已满" : picked >= budget ? "预算已满" : "入选";
      if (why.equals("入选")) {
        picked++;
        used.put(v.group(), n + 1);
      }
      evidence.add(Map.of("id", v.id(), "group", v.group(), "entropy", v.entropy(), "reason", why));
    }
    return result(Map.of("picked", picked, "unused", budget - picked), evidence);
  }
}
