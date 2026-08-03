package com.maplemetric.internal.application.service;

public class OverallRankingBackfillJobNotFoundException
        extends RuntimeException {

    public OverallRankingBackfillJobNotFoundException() {
        super("Backfill Job을 찾을 수 없습니다.");
    }
}
