package com.maplemetric.analysis.domain.model;

/**
 * 수치의 단위다.
 *
 * {@code PERCENT}와 {@code PERCENTAGE_POINT}를 나눠 둔다. 합치면 "10%에서 12%로
 * 올랐다"를 20% 증가로도 2%p 증가로도 읽을 수 있어 설명이 어긋난다.
 */
public enum StatisticsInsightUnit {
    PERCENT,
    PERCENTAGE_POINT,
    COUNT,
    LEVEL
}
