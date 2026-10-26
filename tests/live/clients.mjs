import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { createInterface } from 'node:readline';
import { pathToFileURL } from 'node:url';

// Dependencies live in the ignored build directory, not beside these sources.
const require = createRequire(pathToFileURL(process.argv[2]));
const { createBot } = require('mineflayer');
const bots = new Map();

async function bounded(action, ms = 30000) {
  let timer;
  try {
    return await Promise.race([action, new Promise((_, reject) => {
      timer = setTimeout(() => reject(new Error('client timeout')), ms);
    })]);
  } finally {
    clearTimeout(timer);
  }
}

async function dispatch(request) {
  if (request.op === 'connect') {
    assert(Number.isInteger(request.port) && request.port > 0 && request.port < 65536);
    assert(/^[A-Za-z0-9_]{1,16}$/.test(request.name));
    assert(!bots.has(request.name));
    const bot = createBot({
      host: '127.0.0.1', port: request.port, username: request.name,
      version: '1.21.8', auth: 'offline', hideErrors: true,
    });
    bots.set(request.name, bot);
    bot.physicsEnabled = false;
    bot.on('error', () => {});
    await bounded(new Promise((resolve, reject) => {
      bot.once('spawn', resolve);
      bot.once('error', reject);
      bot.once('kicked', () => reject(new Error('client kicked')));
      bot.once('end', () => reject(new Error('client disconnected')));
    }));
    return { spawned: request.name, uuid: bot.player.uuid };
  }
  if (request.op === 'chat') {
    const bot = bots.get(request.name);
    assert(bot && typeof request.command === 'string' && !/[\r\n]/.test(request.command));
    let listener;
    try {
      const received = request.expect ? new Promise(resolve => {
        listener = message => {
          if (message.includes(request.expect)) resolve();
        };
        bot.on('messagestr', listener);
      }) : Promise.resolve();
      bot.chat(request.command);
      await bounded(received, 10000);
      return { sent: true, responseMatched: Boolean(request.expect) };
    } finally {
      if (listener) bot.removeListener('messagestr', listener);
    }
  }
  if (request.op === 'quit') {
    for (const bot of bots.values()) bot.quit();
    bots.clear();
    return { closed: true };
  }
  throw new Error('unknown client operation');
}

try {
  for await (const line of createInterface({ input: process.stdin })) {
    let request;
    try {
      request = JSON.parse(line);
      const result = await dispatch(request);
      console.log(JSON.stringify({ id: request.id, ok: true, ...result }));
    } catch (error) {
      // Do not echo input, server messages, links or credentials to the parent.
      console.log(JSON.stringify({ id: request?.id, ok: false, error: error.name }));
    }
  }
} finally {
  for (const bot of bots.values()) bot.quit();
}
