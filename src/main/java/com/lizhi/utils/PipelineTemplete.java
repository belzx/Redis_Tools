package com.lizhi.utils;

import java.util.List;

public interface PipelineTemplete<T> {
    void pipelineExecute();

    T processResult(List<Object> objects);
}
