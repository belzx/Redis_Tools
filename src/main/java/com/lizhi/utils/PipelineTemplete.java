package com.lizhi.utils;

import java.util.List;

/**
 * @author https://github.com/lizhixiong1994
 * @Date 2019-02-28
 */
public interface PipelineTemplete {
    void pipelineExecute();

    void resultProcess(List<Object> objects);
}
