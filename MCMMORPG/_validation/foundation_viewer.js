function startViewer(bot, options = {}) {
  if (!options.enabled) {
    return { close() {}, url: '' };
  }

  const { mineflayer: mineflayerViewer } = require('prismarine-viewer');
  const port = Number(options.port || process.env.FOUNDATION_VIEWER_PORT || 3000);
  const firstPerson = options.firstPerson !== false;
  const viewDistance = Number(options.viewDistance || 6);

  mineflayerViewer(bot, {
    port,
    firstPerson,
    viewDistance
  });

  if (bot.viewer?.drawLine) {
    const path = [];
    bot.on('move', () => {
      if (!bot.entity) return;
      const pos = bot.entity.position.clone();
      if (!path.length || path[path.length - 1].distanceTo(pos) > 1) {
        path.push(pos);
        bot.viewer.drawLine('foundation-path', path, 0x00ff88);
      }
    });
  }

  return {
    url: `http://127.0.0.1:${port}`,
    close() {
      try {
        bot.viewer?.close?.();
      } catch {}
    }
  };
}

module.exports = { startViewer };
