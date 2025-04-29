---
description: >-
  Installing CommandBridge is quick and simple. Follow these steps to get
  started
---

# 📩 Installation

### Setup **the Plugin**

1. Download the latest `CommandBridge-xxx-all.jar` from the [releases page](https://modrinth.com/plugin/commandbridge/versions).
2. Place the JAR file into the `plugins` folder on **both your Velocity and Paper servers**.
3. Restart **both** the **Velocity and Paper** server and let the plugin generate its configs.&#x20;

{% hint style="success" %}
No need for separate JARs - CommandBridge will automatically detect whether it’s running on Velocity or Paper.
{% endhint %}

***

### **Upgrading to Version 2.0.0**

If you’re upgrading from an older version of CommandBridge to version **2.0.0 or above**, follow these additional steps:

1. **Convert Scripts Manually**:
   * The script format has changed. You’ll need to manually convert your old scripts to the new format. You can see how to write the scripts [here](broken-reference).
2. **Regenerate `config.yml`**:
   * Delete your old `config.yml`.
   * Start the server to let the plugin generate a new configuration file.
   * Follow the [guide](broken-reference) on how to configure the plugin.

{% hint style="info" %}
Future updates will include an automatic script converter for easier upgrades.
{% endhint %}

