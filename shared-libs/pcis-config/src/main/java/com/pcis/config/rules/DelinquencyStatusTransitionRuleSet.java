package com.pcis.config.rules;

import java.util.List;
import java.util.Optional;

/** Immutable view of billing schedule delinquency status transitions. */
public record DelinquencyStatusTransitionRuleSet(List<Transition> transitions) {

  public DelinquencyStatusTransitionRuleSet {
    transitions = List.copyOf(transitions);
  }

  public static DelinquencyStatusTransitionRuleSet fromPayload(List<Transition> transitions) {
    return new DelinquencyStatusTransitionRuleSet(transitions);
  }

  public Optional<String> resolveNextStatus(String fromStatus, String event) {
    return transitions.stream()
        .filter(transition -> transition.fromStatus().equals(fromStatus) && transition.event().equals(event))
        .map(Transition::toStatus)
        .findFirst();
  }

  public record Transition(String fromStatus, String event, String toStatus) {}
}
