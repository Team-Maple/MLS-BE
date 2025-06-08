package com.maple.api.auth.application.dto;

import com.maple.api.auth.domain.Provider;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateMemberRequestDto {
    private String providerId;
  private String name;
}
