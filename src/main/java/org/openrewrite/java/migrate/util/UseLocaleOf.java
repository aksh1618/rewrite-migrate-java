/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.util;

import org.openrewrite.Applicability;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.StringJoiner;

public class UseLocaleOf extends Recipe {
    private static final MethodMatcher NEW_LOCALE = new MethodMatcher("java.util.Locale <constructor>(..)", false);

    @Override
    public String getDisplayName() {
        return "Prefer `Locale.of(..)` over `new Locale(..)`";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public String getDescription() {
        return "Prefer `Locale.of(..)` over `new Locale(..)` in Java 19 or higher.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.and(
                new UsesJavaVersion<>(19),
                new UsesMethod<>(NEW_LOCALE));
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (NEW_LOCALE.matches(nc)) {
                    StringJoiner localeOf = new StringJoiner(", ", "Locale.of(", ")");
                    nc.getArguments().forEach(a -> localeOf.add("#{any(String)}"));
                    return autoFormat(nc.withTemplate(
                            JavaTemplate
                                    .builder(this::getCursor, localeOf.toString())
                                    .imports("java.util.Locale")
                                    .build(),
                            nc.getCoordinates().replace(),
                            nc.getArguments().toArray()
                    ), ctx);
                }
                return nc;
            }
        };
    }
}
