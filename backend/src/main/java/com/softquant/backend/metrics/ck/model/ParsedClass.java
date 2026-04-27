package com.softquant.backend.metrics.ck.model;

import java.util.List;
import java.util.Set;

public record ParsedClass(
        String className,
        String superClassName,
        Set<String> fields,
        Set<String> directClassReferences,
        List<ParsedMethod> methods
) {
}
