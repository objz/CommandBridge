# 📃 Placeholders

CommandBridge supports dynamic **placeholders** inside your command strings!

#### Built-in placeholders

| Placeholder   | Value                                   |
| ------------- | --------------------------------------- |
| `%cb_player%` | Player's name who triggered the script. |
| `%cb_uuid%`   | Player's UUID.                          |
| `%cb_world%`  | (Paper only) Player's world name.       |

#### Example:

```yaml
- command: "say %cb_player% has initiated the event!"
  target-executor: "console"
```

Displays:

> "Alex has initiated the event!"

#### PlaceholderAPI Integration

* If you have **PlaceholderAPI** (Paper) or **PapiProxyBridge** (Velocity), **ANY PAPI placeholder** will work too.

{% hint style="info" %}
Without PAPI, only `%cb_...%` placeholders are available.
{% endhint %}
