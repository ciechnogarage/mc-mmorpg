'use strict';

// Kolejka zadań z priorytetami (plan E). Priorytety wg zasady:
// integralność > stabilność > wydajność > gameplay.

const PRIORITIES = { integrity: 0, stability: 1, performance: 2, gameplay: 3 };

class TaskQueue {
  constructor({ log = console } = {}) {
    this.log = log;
    this.queue = [];
    this.running = false;
    this.current = null;
    this.results = [];
  }

  push(task) {
    const { name, run, priority = 'gameplay', timeoutMs = 120000 } = task;
    if (typeof run !== 'function') throw new Error('task.run musi być funkcją');
    const prio = PRIORITIES[priority] ?? PRIORITIES.gameplay;
    this.queue.push({ name, run, prio, timeoutMs, enqueuedAt: Date.now() });
    this.queue.sort((a, b) => a.prio - b.prio || a.enqueuedAt - b.enqueuedAt);
    this._drain();
    return this;
  }

  async _drain() {
    if (this.running) return;
    this.running = true;
    while (this.queue.length) {
      const task = this.queue.shift();
      this.current = task;
      const started = Date.now();
      try {
        const value = await Promise.race([
          task.run(),
          new Promise((_, reject) => setTimeout(() => reject(new Error('task timeout')), task.timeoutMs))
        ]);
        this.results.push({ name: task.name, status: 'ok', ms: Date.now() - started, value });
      } catch (error) {
        this.results.push({ name: task.name, status: 'error', ms: Date.now() - started, error: error.message });
        this.log.error(`[task-queue] ${task.name}: ${error.message}`);
      }
      this.current = null;
    }
    this.running = false;
  }

  async idle() {
    while (this.running || this.queue.length) {
      await new Promise(r => setTimeout(r, 100));
    }
    return this.results;
  }
}

module.exports = { TaskQueue, PRIORITIES };
