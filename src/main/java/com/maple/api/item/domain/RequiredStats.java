package com.maple.api.item.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequiredStats {

    @Column(name = "required_level")
    private Integer level;

    @Column(name = "required_str")
    private Integer str;

    @Column(name = "required_dex")
    private Integer dex;

    /**
     * int는 자바 키워드라 변수명으로 사용 불가
      */
    @Column(name = "required_int")
    private Integer intelligence;

    @Column(name = "required_luk")
    private Integer luk;

    @Column(name = "required_pop")
    private Integer pop;
}