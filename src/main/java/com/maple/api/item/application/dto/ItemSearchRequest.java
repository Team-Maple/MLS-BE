package com.maple.api.item.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ItemSearchRequest {
    
    private String keyword;
    
    private Integer job;
    
    @Min(value = 1, message = "최소 레벨은 1 이상이어야 합니다")
    private Integer minLevel = 1;
    
    @Max(value = 200, message = "최대 레벨은 200 이하여야 합니다")
    private Integer maxLevel = 200;
    
    private List<Integer> categories;
}