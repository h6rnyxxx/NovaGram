# NovaPlugins Developer Guide

NovaGram ships with **NovaPlugins**, a lightweight, sandboxed JavaScript (ES6 via Mozilla Rhino) plugin system.

Plugins use the `.nvplugin` (or `.nv.js`) file extension and can be imported from **NovaGram Settings → NovaPlugins → Import Plugin**, or directly from any Telegram chat via the message long-press menu (**Install NovaPlugin**).

---

## Metadata Header

Declare plugin metadata at the top of your file with `// @tag value`:

```javascript
// @id my_first_plugin
// @name My First Plugin
// @version 1.0.0
// @author YourName
// @description Adds a .hello command and message action.
// @permissions ui, storage, commands, messages, network
```

Available permissions:
- `ui` — toasts, bulletins, alerts, clipboard
- `storage` — per-plugin persistent key-value storage
- `commands` — `.command` registration in chat input
- `messages` — `onSendMessage` hooks & message context menu actions
- `network` — enables `nova.net.get(url, callback)`

---

## API Reference (`nova`)

### `nova.plugin`
Read-only metadata: `{ id, name, version, author }`.

### `nova.ui`
- `nova.ui.toast(text)` — shows a non-intrusive bulletin/toast.
- `nova.ui.alert(title, message)` — displays a modal dialog.
- `nova.ui.copy(text)` — copies text to the system clipboard.

### `nova.storage`
- `nova.storage.get(key, defaultValue)`
- `nova.storage.getBoolean(key, defaultValue)`
- `nova.storage.getString(key, defaultValue)`
- `nova.storage.getInt(key, defaultValue)`
- `nova.storage.set(key, value)`
- `nova.storage.remove(key)`

### `nova.settings`
Automatically renders a native settings page inside **NovaPlugins → Plugin Details**:

```javascript
nova.settings.define([
  { type: 'header', title: 'General' },
  { type: 'switch', key: 'enabled', title: 'Enable Feature', default: true },
  { type: 'text', key: 'prefix', title: 'Prefix', default: '[Nova]' },
  { type: 'button', title: 'Test Action', onClick: function() { nova.ui.toast('Clicked!'); } }
]);
```

### `nova.commands`
Register chat commands triggered with `.command`:

```javascript
nova.commands.register('ping', 'Check plugin engine', function(ctx) {
  nova.ui.toast('Pong! Dialog: ' + ctx.dialogId);
  return { cancel: true }; // cancels sending the message to Telegram
});

nova.commands.register('shrug', 'Append shrug', function(ctx) {
  return { text: (ctx.args ? ctx.args + ' ' : '') + '¯\\_(ツ)_/¯' };
});
```

### `nova.hooks`
- `nova.hooks.onSendMessage(function(ctx) { ... })` — receives `{ dialogId, text, args }`. Return `{ text: "..." }` to modify or `{ cancel: true }` to abort sending.

### `nova.menu`
- `nova.menu.registerMessageAction(id, title, function(msg) { ... })` — adds an item to the message long-press menu. `msg` contains `{ id, dialogId, text, isOut, date }`.

### `nova.net` *(requires `network` permission)*
- `nova.net.get(url, function(error, body) { ... })` — async HTTP GET returning response on the UI thread.

### `nova.device`
- `nova.device.getModel()`
- `nova.device.getSdkInt()`
- `nova.device.getNovaVersion()`
