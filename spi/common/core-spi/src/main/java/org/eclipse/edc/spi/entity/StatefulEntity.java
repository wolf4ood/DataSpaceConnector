/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *       Microsoft Corporation - added fields
 *
 */

package org.eclipse.edc.spi.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.edc.spi.telemetry.TraceCarrier;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for state machine persistent entities.
 *
 * @param <T> implementation type ({@link StatefulEntity} sub-class). Used to define {@link #copy()} method.
 */
public abstract class StatefulEntity<T extends StatefulEntity<T>> extends MutableEntity implements TraceCarrier {
    protected int state;
    protected int stateCount;
    protected long stateTimestamp;
    protected Map<String, String> traceContext = new HashMap<>();
    protected String errorDetail;
    protected boolean pending = false;
    protected List<Integer> previousStates = new ArrayList<>();

    protected StatefulEntity() {
    }

    public int getState() {
        return state;
    }

    public int getStateCount() {
        return stateCount;
    }

    public long getStateTimestamp() {
        return stateTimestamp;
    }

    @Override
    public Map<String, String> getTraceContext() {
        return Collections.unmodifiableMap(traceContext);
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    /**
     * Sets the state timestamp to the clock time.
     *
     * @see Builder#clock(Clock)
     */
    public void updateStateTimestamp() {
        stateTimestamp = clock.millis();
    }

    public abstract T copy();

    /**
     * Return the string representation of the current state
     *
     * @return the string representing the state.
     */
    public abstract String stateAsString();

    protected void transitionTo(int targetState) {
        stateCount = state == targetState ? stateCount + 1 : 1;
        if (previousStates == null) {
            previousStates = new ArrayList<>();
        }
        if (state != targetState) {
            previousStates.add(state);
        }
        state = targetState;
        updateStateTimestamp();
        setModified();
    }

    /**
     * Returns the list of previous states.
     * This is used to track the history of state transitions.
     *
     * @return a list of previous states.
     */
    public List<Integer> getPreviousStates() {
        return previousStates;
    }

    /**
     * Returns the last previous state.
     *
     * @return the last previous state, or null if there are no previous states.
     */
    @JsonIgnore
    public Integer getLastPreviousState() {
        if (previousStates == null || previousStates.isEmpty()) {
            return null;
        }
        return previousStates.get(previousStates.size() - 1);
    }

    protected <B extends Builder<T, B>> T copy(Builder<T, B> builder) {
        return builder
                .id(id)
                .createdAt(createdAt)
                .state(state)
                .stateCount(stateCount)
                .stateTimestamp(stateTimestamp)
                .updatedAt(updatedAt)
                .traceContext(traceContext)
                .errorDetail(errorDetail)
                .clock(clock)
                .pending(pending)
                .previousStates(previousStates)
                .build();
    }

    /**
     * Base Builder class for derived classes.
     *
     * @param <T> type being built ({@link StatefulEntity} sub-class)
     * @param <B> derived Builder ({@link Builder} sub-class)
     * @see <a href="http://egalluzzo.blogspot.com/2010/06/using-inheritance-with-fluent.html">Using inheritance with fluent interfaces (blog post)</a> for a background on the use of generic types.
     */
    protected abstract static class Builder<T extends StatefulEntity<T>, B extends Builder<T, B>> extends MutableEntity.Builder<T, B> {

        protected Builder(T entity) {
            super(entity);
        }

        public B state(int value) {
            entity.state = value;
            return self();
        }

        public B stateCount(int value) {
            entity.stateCount = value;
            return self();
        }

        public B stateTimestamp(long value) {
            entity.stateTimestamp = value;
            return self();
        }

        public B errorDetail(String errorDetail) {
            entity.errorDetail = errorDetail;
            return self();
        }

        public B traceContext(Map<String, String> traceContext) {
            entity.traceContext = traceContext;
            return self();
        }

        public B pending(boolean pending) {
            entity.pending = pending;
            return self();
        }

        public B previousStates(List<Integer> previousStates) {
            entity.previousStates = previousStates;
            return self();
        }

        protected T build() {
            super.build();
            if (entity.id == null) {
                entity.id = UUID.randomUUID().toString();
            }

            if (entity.stateTimestamp == 0) {
                entity.stateTimestamp = entity.clock.millis();
            }

            if (entity.previousStates == null) {
                entity.previousStates = new ArrayList<>();
            }

            return entity;
        }
    }

}
