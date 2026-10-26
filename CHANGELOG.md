# CommandBridge 3.3.7

## Added

- Live web script editor via `/cb editor`, with changes reviewed and confirmed through `/cb editor confirm <code>` before saving and reloading scripts.
- Guided network setup via `/cb setup`, with console confirmation before applying configuration changes.
- Backend pairing via `/cbc pair`, including a destination preview and console confirmation before saving connection settings.
- Backend platform information in the proxy's server inventory.

## Security and reliability

- End-to-end encryption for browser editor, setup and pairing messages.
- Signed Redis messages with channel binding, expiry and replay checks.
- Authentication gates for incoming connections and size limits for bridge and relay messages.
- Fixes for reconnect/authentication races, stale command registrations, remote-player permissions and dispatch validation.
- Safer script/configuration and scheduled-task persistence, preserving unreadable task data instead of overwriting it.
- Fixes for oversized time arguments, optional numeric arguments, plugin-message queues and diagnostics redaction.

## Upgrade notes

- **Redis networks: update and restart every connected proxy and backend together.** Older unsigned Redis messages are rejected by this version. Keep system clocks within two minutes of each other.
- Back up your configuration and scripts before updating. Keep editor/setup links and pairing codes private, and verify the destination before confirming a backend pairing.
