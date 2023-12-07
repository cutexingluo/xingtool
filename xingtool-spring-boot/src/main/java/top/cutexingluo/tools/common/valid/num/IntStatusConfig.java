package top.cutexingluo.tools.common.valid.num;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author XingTian
 * @version 1.0.0
 * @date 2023/7/19 17:21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntStatusConfig {

    boolean notNull = false;

    int[] matchNum = {};

    boolean limit = false;

    int min = 0;

    int max = Integer.MAX_VALUE;


}
