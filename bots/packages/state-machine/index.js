'use strict';

// Własna FSM zamiast porzuconego mineflayer-statemachine (plan A/E).
// Cykl bota: idle -> task -> verify -> report -> idle.

class StateMachine {
  constructor({ initial = 'idle', transitions, log = console } = {}) {
    // transitions: { stan: [dozwolone stany docelowe] }
    this.transitions = transitions || {
      idle: ['task'],
      task: ['verify', 'idle'],
      verify: ['report', 'task'],
      report: ['idle']
    };
    this.state = initial;
    this.log = log;
    this.listeners = [];
    this.history = [{ state: initial, at: Date.now() }];
  }

  can(next) {
    return (this.transitions[this.state] || []).includes(next);
  }

  go(next, context = {}) {
    if (!this.can(next)) throw new Error(`niedozwolone przejście: ${this.state} -> ${next}`);
    const prev = this.state;
    this.state = next;
    this.history.push({ state: next, at: Date.now() });
    if (this.history.length > 200) this.history.shift();
    for (const fn of this.listeners) fn(next, prev, context);
    return this;
  }

  onTransition(fn) {
    this.listeners.push(fn);
    return this;
  }
}

module.exports = { StateMachine };
