package com.maple.api.alrim.domain;

public enum AlrimType {
  PATCH_NOTE("패치노트"),
  EVENT("이벤트"),
  NOTICE("공지사항");

  private final String label;

  AlrimType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}