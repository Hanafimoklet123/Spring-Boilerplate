package com.boilerplate.spring_boot.commons.interceptor;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MaskingDataRule {

    private String field;
    private String[] fieldTree;

    @SerializedName("start_index_masking")
    private int startIndexMasking;
}
