package com.softquant.backend.metrics.ck.model;

import java.util.Set;

public record ParsedMethod(
        String methodName,
        Set<String> referencedFields,
        Set<String> referencedClasses,
        Set<String> externalMethodCalls
) {
}
