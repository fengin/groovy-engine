package cn.fengin.groovy.demo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 示例业务 Bean — 演示脚本如何调用 Java 服务
 * <p>
 * 提供几个简单的方法，脚本中通过 {@code ctx.demoService.xxx()} 调用。
 *
 * @author 凌封 (https://aibook.ren)
 */
public class DemoService {

    /**
     * 获取当前时间（格式化）
     */
    public String now(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            pattern = "yyyy-MM-dd HH:mm:ss";
        }
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 简单数学计算
     */
    public BigDecimal calculate(BigDecimal a, BigDecimal b, String operator) {
        if (a == null || b == null) return BigDecimal.ZERO;
        switch (operator != null ? operator : "+") {
            case "+": return a.add(b);
            case "-": return a.subtract(b);
            case "*": return a.multiply(b);
            case "/": return b.compareTo(BigDecimal.ZERO) != 0
                    ? a.divide(b, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            default: return BigDecimal.ZERO;
        }
    }

    /**
     * 模拟数据查询
     */
    public List<Map<String, Object>> queryData(String category) {
        List<Map<String, Object>> result = new ArrayList<>();
        Random random = new Random();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", i);
            row.put("name", (category != null ? category : "item") + "-" + i);
            row.put("value", random.nextInt(1000));
            row.put("time", now("yyyy-MM-dd HH:mm:ss"));
            result.add(row);
        }
        return result;
    }
}
