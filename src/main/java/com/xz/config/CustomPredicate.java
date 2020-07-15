package com.xz.config;

import org.springframework.stereotype.Component;

import java.util.function.Predicate;
@Component
public class CustomPredicate implements Predicate {
    @Override
    public boolean test(Object o) {
        return true;
    }
}
