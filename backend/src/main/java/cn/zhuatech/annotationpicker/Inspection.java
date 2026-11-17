package cn.zhuatech.annotationpicker;

import static cn.zhuatech.annotationpicker.Checks.*;

import java.util.*;

/**
 * 列出每个来源的入选编号、未选数量和熵排序最高的候补。候补仅用于该来源有名额时替换，不自动突破预算。 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
 * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
final class Inspection {
  /**
   * 有序结果，可表达无法估计的数值。上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
   * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
   */
  private static Map<String, Object> fields(Object... pairs) {
    var row = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) row.put((String) pairs[i], pairs[i + 1]);
    return row;
  }

  /**
   * 对已校验的输入生成复核证据。上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
   * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
   */
  @SuppressWarnings("unchecked")
  static Map<String, Object> evaluate(
      Map<String, Object> cfg, List<Map<String, Object>> rows, Map<String, Object> base) {
    var details = new ArrayList<Map<String, Object>>();

    var output = (List<Map<String, Object>>) base.get("rows");
    var groups = new TreeMap<String, List<Map<String, Object>>>();
    for (var v : output)
      groups.computeIfAbsent((String) v.get("group"), k -> new ArrayList<>()).add(v);
    int covered = 0;
    for (var g : groups.entrySet()) {
      var picked = new ArrayList<String>();
      String reserve = null;
      for (var v : g.getValue()) {
        if ("入选".equals(v.get("reason"))) picked.add((String) v.get("id"));
        else if (reserve == null) reserve = (String) v.get("id");
      }
      if (!picked.isEmpty()) covered++;
      details.add(
          fields(
              "group",
              g.getKey(),
              "pickedIds",
              picked,
              "pickedCount",
              picked.size(),
              "waiting",
              g.getValue().size() - picked.size(),
              "reserveId",
              reserve));
    }
    return result(fields("coveredGroups", covered, "totalGroups", groups.size()), details);
  }
}
