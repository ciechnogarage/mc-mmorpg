export class BatchSafetyError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'BatchSafetyError';
    this.code = code;
  }
}

export function batchFail(code, message) {
  throw new BatchSafetyError(code, message);
}
