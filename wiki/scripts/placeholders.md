# 📃 Placeholders

CommandBridge supports dynamic **placeholders** in your command strings.

They allow you to inject player-specific data like name, UUID, or server context directly into scripted commands.

---

### 🔠 Internal Placeholders

These are built-in and always available without any external plugins:

| Placeholder     | Paper Support | Velocity Support | Description                                |
|----------------|:-------------:|:----------------:|--------------------------------------------|
| `%cb_player%`   | ✅             | ✅                | Player's name or username.                 |
| `%cb_uuid%`     | ✅             | ✅                | Player's unique UUID.                      |
| `%cb_world%`    | ✅             | ❌                | The world name the player is in.           |
| `%cb_server%`   | ❌             | ✅                | The server name the player is connected to.|

{% hint style="info" %}
Paper-only placeholders like `%cb_world%` and Velocity-only placeholders like `%cb_server%` will be ignored on the other side.
{% endhint %}

---

### 🔌 PlaceholderAPI Support

CommandBridge also supports full **PlaceholderAPI (PAPI)** integration:

- On **Paper**, just install **PlaceholderAPI**.
- On **Velocity**, install **PapiProxyBridge**.

This enables **any PAPI placeholder** (like `%luckperms_prefix%`, `%vault_eco_balance%`, etc.).

```yaml
- command: "say Welcome %luckperms_prefix%%cb_player%!"
```

> Displays: `Welcome [Admin] Alex!`

{% hint style="info" %}
Using PAPI is **optional** — but highly recommended for dynamic placeholders beyond the built-ins.
{% endhint %}
