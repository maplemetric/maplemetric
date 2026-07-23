package com.maplemetric.analysis.application.service;

import com.maplemetric.analysis.application.result.InsightResult;
import com.maplemetric.analysis.domain.model.InsightFacts;

public interface InsightGenerator {

    InsightResult generate(InsightFacts facts);
}
