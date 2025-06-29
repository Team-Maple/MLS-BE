package com.maple.api.item.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("OTHER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OtherItem extends Item {

}