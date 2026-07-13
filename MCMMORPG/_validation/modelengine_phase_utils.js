const PHASES = ['start', 'anticipation', 'impact', 'recovery', 'end'];

function collectAnimationFrames(animation) {
  return Object.values(animation?.animators || {}).flatMap((animator) => animator.keyframes || []);
}

function motionEnergy(frame) {
  return (frame.data_points || []).reduce((sum, point) => sum +
    ['x', 'y', 'z'].reduce((axisSum, axis) => axisSum + Math.abs(Number(point?.[axis]) || 0), 0), 0);
}

function deriveAnimationPhases(keyframes, length) {
  const safeLength = Number(length || 0);
  const timelineTimes = keyframes
    .filter((frame) => frame.channel === 'timeline')
    .map((frame) => Number(frame.time || 0))
    .filter((time) => Number.isFinite(time) && time >= 0 && time <= safeLength);
  const motionFrames = keyframes
    .filter((frame) => ['rotation', 'position', 'scale'].includes(frame.channel))
    .map((frame) => ({
      time: Number(frame.time || 0),
      energy: motionEnergy(frame),
    }))
    .filter((frame) => Number.isFinite(frame.time) && frame.time > 0 && frame.time < safeLength);

  let impact;
  let method;
  if (timelineTimes.length) {
    impact = [...timelineTimes].sort((a, b) => Math.abs(a - safeLength * 0.6) - Math.abs(b - safeLength * 0.6))[0];
    method = 'timeline';
  } else if (motionFrames.length) {
    impact = [...motionFrames].sort((a, b) => b.energy - a.energy || a.time - b.time)[0].time;
    method = 'motion_peak';
  } else {
    impact = safeLength * 0.8;
    method = 'percentage_fallback';
  }

  const distinctTimes = [...new Set(
    keyframes
      .map((frame) => Number(frame.time || 0))
      .filter((time) => Number.isFinite(time) && time >= 0 && time <= safeLength),
  )].sort((a, b) => a - b);
  const before = distinctTimes.filter((time) => time < impact);
  const after = distinctTimes.filter((time) => time > impact);
  const anticipation = before.length ? before[Math.max(0, before.length - 2)] : impact * 0.5;
  const postImpactMotion = motionFrames.filter((frame) => frame.time > impact);
  const recovery = postImpactMotion.length
    ? [...postImpactMotion].sort((a, b) => a.energy - b.energy || a.time - b.time)[0].time
    : after.length
      ? after[Math.min(after.length - 1, 1)]
      : impact + (safeLength - impact) * 0.5;

  return {
    method,
    start: 0,
    anticipation: Number(Math.max(0, anticipation).toFixed(4)),
    impact: Number(Math.min(safeLength, impact).toFixed(4)),
    recovery: Number(Math.min(safeLength, recovery).toFixed(4)),
    end: Number(safeLength.toFixed(4)),
  };
}

function animationPhaseTimesFromAnimation(animation) {
  const keyframes = collectAnimationFrames(animation);
  const length = Number(animation?.length || Math.max(0, ...keyframes.map((frame) => Number(frame.time || 0))));
  return deriveAnimationPhases(keyframes, length);
}

module.exports = {
  PHASES,
  collectAnimationFrames,
  deriveAnimationPhases,
  animationPhaseTimesFromAnimation,
};
