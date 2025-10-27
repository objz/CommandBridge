const { createApp, ref, reactive, computed, watch, onMounted, onUnmounted } = Vue;

const app = createApp({
	template: `
        <div class="app-container">
            <header class="app-header">
                <div class="header-content">
                    <h1>Middleman</h1>
                </div>
                <div class="status-indicator">
                    <span class="dot" :class="connected ? 'connected' : 'disconnected'"></span>
                    <span class="status-text">{{ connected ? 'Connected' : 'Disconnected' }}</span>
                </div>
            </header>

            <div class="alerts-container">
                <div v-for="(alert, idx) in alerts" :key="idx" class="alert" :class="alert.type">
                    {{ alert.message }}
                    <button @click="alerts.splice(idx, 1)" class="alert-close">&times;</button>
                </div>
            </div>

            <main class="main-layout">
                <aside class="sidebar">
                    <div class="sidebar-section">
                        <h2>TLS Setup</h2>
                        <div class="form-group">
                            <label>keystore.p12</label>
                            <input 
                                @change="handleKeystoreFile" 
                                type="file" 
                                accept=".p12,.pfx"
                                ref="keystoreInput"
                            >
                        </div>
                        <div class="form-group">
                            <label>keystore.pass</label>
                            <input 
                                @change="handlePasswordFile" 
                                type="file" 
                                accept=".pass,.txt"
                                ref="passwordInput"
                            >
                        </div>
                        <div v-if="tlsStatus" class="tls-status">
                            <span :class="tlsStatus.ok ? 'success' : 'error'">
                                {{ tlsStatus.message }}
                            </span>
                        </div>
                        <button @click="loadTLS" class="btn btn-secondary" :disabled="!tlsReady">
                            Load TLS
                        </button>
                    </div>

                    <div class="sidebar-section">
                        <h2>Statistics</h2>
                        <div class="stats">
                            <div class="stat">
                                <span class="label">Messages:</span>
                                <span class="value">{{ messageCount }}</span>
                            </div>
                            <div class="stat">
                                <span class="label">Client to Server:</span>
                                <span class="value">{{ stats.clientToServer }}</span>
                            </div>
                            <div class="stat">
                                <span class="label">Server to Client:</span>
                                <span class="value">{{ stats.serverToClient }}</span>
                            </div>
                            <div class="stat" v-if="currentMode.operation === 'EDIT'">
                                <span class="label">Held Messages:</span>
                                <span class="value">{{ heldMessages.length }}</span>
                            </div>
                            <div class="stat">
                                <span class="label">Session Duration:</span>
                                <span class="value">{{ sessionDuration }}</span>
                            </div>
                        </div>
                        <button @click="clearLogs" class="btn btn-danger btn-small">Clear Logs</button>
                    </div>
                </aside>

                <section class="content">
                    <div class="mode-selector">
                        <div class="mode-group">
                            <h3>Encryption Mode</h3>
                            <div class="button-group">
                                <button 
                                    v-for="mode in ['PLAIN', 'ENCRYPTED']"
                                    :key="mode"
                                    @click="setEncryptionMode(mode)"
                                    :class="['mode-btn', currentMode.encryption === mode && 'active']"
                                >
                                    {{ mode }}
                                </button>
                            </div>
                        </div>

                        <div class="mode-group">
                            <h3>Operation Mode</h3>
                            <div class="button-group">
                                <button 
                                    v-for="op in ['FORWARD', 'EDIT']"
                                    :key="op"
                                    @click="setOperationMode(op)"
                                    :class="['mode-btn', currentMode.operation === op && 'active']"
                                >
                                    {{ op }}
                                </button>
                            </div>
                        </div>
                    </div>

                    <div v-if="currentMode.operation === 'EDIT' && heldMessages.length > 0" class="held-messages-actions">
                        <button @click="sendAllHeldMessages" class="btn btn-primary">Send All ({{ heldMessages.length }})</button>
                        <button @click="clearHeldMessages" class="btn btn-danger">Clear All Held</button>
                    </div>

                    <div class="content-split">
                        <div class="messages-panel">
                            <div class="panel-header">
                                <h2>{{ currentMode.operation === 'EDIT' ? 'Held Messages' : 'Messages' }}</h2>
                                <div class="filter-controls">
                                    <input 
                                        v-model="messageFilter" 
                                        type="text" 
                                        placeholder="Filter messages..."
                                    >
                                    <select v-model="directionFilter">
                                        <option value="">All Directions</option>
                                        <option value="client→server">Client to Server</option>
                                        <option value="server→client">Server to Client</option>
                                    </select>
                                    <label v-if="currentMode.encryption === 'ENCRYPTED'" class="toggle-encrypted">
                                        <input type="checkbox" v-model="showEncrypted">
                                        <span>Show Encrypted</span>
                                    </label>
                                </div>
                            </div>

                            <div class="messages-list">
                                <div 
                                    v-for="(msg, idx) in displayedMessages" 
                                    :key="msg.uniqueId || idx"
                                    @click="selectMessage(msg)"
                                    :class="['message-item', msg.direction.includes('client') ? 'c2s' : 's2c', selectedMessage && selectedMessage.uniqueId === msg.uniqueId && 'selected', msg.isHeld && 'held']"
                                >
                                    <div class="message-header-line">
                                        <div class="message-meta-left">
                                            <span class="msg-number">#{{ msg.displayNumber }}</span>
                                            <span class="timestamp">{{ formatTime(msg.timestamp) }}</span>
                                            <span class="client-info" v-if="msg.fromClient && msg.toClient">
                                                '{{ msg.fromClient }}' → '{{ msg.toClient }}'
                                            </span>
                                        </div>
                                        <div class="message-meta-right">
                                            <span v-if="msg.isHeld" class="held-badge">HELD</span>
                                            <span class="mode-label encryption">{{ msg.encryption_mode }}</span>
                                            <span class="mode-label operation">{{ msg.operation_mode }}</span>
                                        </div>
                                    </div>
                                    <div class="message-preview">
                                        {{ displayMessagePreview(msg) }}
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div v-if="selectedMessage" class="editor-panel">
                            <div class="panel-header">
                                <h2>{{ currentMode.operation === 'EDIT' ? 'Message Editor' : 'Message Details' }}</h2>
                                <div class="header-badges">
                                    <span class="mode-badge">{{ currentMode.operation.toUpperCase() }} MODE</span>
                                    <span class="info-badge">Message #{{ selectedMessage.displayNumber }}</span>
                                    <span v-if="selectedMessage.isHeld" class="held-badge">HELD</span>
                                </div>
                            </div>
                            
                            <div v-if="currentMode.operation === 'EDIT'" class="editor-container">
                                <div class="editor-pane">
                                    <h3>Original Message</h3>
                                    <div class="json-display" v-html="highlightJson(getDisplayContent(selectedMessage))"></div>
                                </div>

                                <div class="editor-pane editable">
                                    <h3>Edited Message</h3>
                                    <textarea 
                                        v-model="editedMessageText"
                                        class="json-editor"
                                        spellcheck="false"
                                    ></textarea>
                                </div>
                            </div>

                            <div v-else class="viewer-container">
                                <div class="message-metadata">
                                    <div class="meta-row">
                                        <span class="meta-label">Encryption:</span>
                                        <span class="meta-value">{{ selectedMessage.encryption_mode }}</span>
                                    </div>
                                    <div class="meta-row">
                                        <span class="meta-label">Size:</span>
                                        <span class="meta-value">{{ formatBytes(selectedMessage.message.length) }}</span>
                                    </div>
                                    <div class="meta-row" v-if="selectedMessage.parsed && selectedMessage.parsed.raw === undefined">
                                        <span class="meta-label">JSON Keys:</span>
                                        <span class="meta-value">{{ Object.keys(selectedMessage.parsed).length }}</span>
                                    </div>
                                </div>
                                
                                <div class="json-display" v-html="highlightJson(getDisplayContent(selectedMessage))"></div>
                            </div>

                            <div v-if="currentMode.operation === 'EDIT' && selectedMessage.isHeld" class="editor-actions">
                                <button @click="sendSelectedMessage" class="btn btn-primary">Send This Message</button>
                                <button @click="resetEdit" class="btn btn-secondary">Reset</button>
                                <button @click="discardSelectedMessage" class="btn btn-danger">Discard</button>
                            </div>
                        </div>
                    </div>
                </section>
            </main>
        </div>
    `,
	setup() {
		const alerts = ref([]);
		const currentMode = reactive({ encryption: 'PLAIN', operation: 'FORWARD' });
		const messages = ref([]);
		const heldMessages = ref([]);
		const selectedMessage = ref(null);
		const messageFilter = ref('');
		const directionFilter = ref('');
		const connected = ref(false);
		const messageCount = ref(0);
		const stats = reactive({ clientToServer: 0, serverToClient: 0 });
		const editedMessageText = ref('');
		const tlsStatus = ref(null);
		const keystoreInput = ref(null);
		const passwordInput = ref(null);
		const tlsFiles = reactive({ keystore: null, password: null });
		const sessionStartTime = ref(Date.now());
		const currentTime = ref(Date.now());
		const showEncrypted = ref(false);

		let durationInterval = null;

		const tlsReady = computed(() => tlsFiles.keystore && tlsFiles.password);

		const sessionDuration = computed(() => {
			const diff = Math.floor((currentTime.value - sessionStartTime.value) / 1000);
			const hours = Math.floor(diff / 3600);
			const minutes = Math.floor((diff % 3600) / 60);
			const seconds = diff % 60;
			return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
		});

		onMounted(() => {
			durationInterval = setInterval(() => {
				currentTime.value = Date.now();
			}, 1000);
		});

		onUnmounted(() => {
			if (durationInterval) {
				clearInterval(durationInterval);
			}
		});

		const extractClientInfo = (msg) => {
			try {
				const parsed = JSON.parse(msg.message);
				if (parsed.from && parsed.to) {
					return {
						fromClient: parsed.from,
						toClient: parsed.to
					};
				}
			} catch (e) {
				// Not JSON or missing fields
			}
			return { fromClient: null, toClient: null };
		};

		const formatTime = (timestamp) => {
			try {
				const date = new Date(timestamp);
				const hours = date.getHours().toString().padStart(2, '0');
				const minutes = date.getMinutes().toString().padStart(2, '0');
				const seconds = date.getSeconds().toString().padStart(2, '0');
				const ms = date.getMilliseconds().toString().padStart(3, '0');
				return `${hours}:${minutes}:${seconds}.${ms}`;
			} catch (e) {
				return timestamp;
			}
		};

		const getDisplayContent = (msg) => {
			if (showEncrypted.value && msg.raw_bytes && currentMode.encryption === 'ENCRYPTED') {
				try {
					return { raw_encrypted: msg.raw_bytes };
				} catch (e) {
					return msg.parsed || { raw: msg.message };
				}
			}
			return msg.parsed || { raw: msg.message };
		};

		const displayMessagePreview = (msg) => {
			if (showEncrypted.value && msg.raw_bytes && currentMode.encryption === 'ENCRYPTED') {
				return msg.raw_bytes.substring(0, 100) + (msg.raw_bytes.length > 100 ? '...' : '');
			}
			return msg.message.substring(0, 100) + (msg.message.length > 100 ? '...' : '');
		};

		const displayedMessages = computed(() => {
			const sourceMessages = currentMode.operation === 'EDIT' ? heldMessages.value : messages.value;
			
			const filtered = sourceMessages.filter(msg => {
				const matchesFilter = msg.message.toLowerCase().includes(messageFilter.value.toLowerCase());
				const matchesDirection = !directionFilter.value || msg.direction === directionFilter.value;
				return matchesFilter && matchesDirection;
			});

			const grouped = [];
			let currentGroup = null;
			
			filtered.forEach((msg) => {
				const msgTime = new Date(msg.timestamp).getTime();
				
				if (!currentGroup || (msgTime - currentGroup.startTime) > 2000) {
					currentGroup = {
						startTime: msgTime,
						messages: []
					};
					grouped.push(currentGroup);
				}
				
				const clientInfo = extractClientInfo(msg);
				const originalIdx = sourceMessages.indexOf(msg);
				
				currentGroup.messages.push({
					...msg,
					...clientInfo,
					originalIdx: originalIdx,
					uniqueId: `${msg.timestamp}-${originalIdx}`,
					groupIndex: currentGroup.messages.length,
					isHeld: msg.isHeld || false
				});
			});

			let displayNumber = 1;
			return grouped.flatMap(group => {
				const groupStart = displayNumber;
				const result = group.messages.map((msg, idx) => ({
					...msg,
					displayNumber: group.messages.length > 1 ? `${groupStart + idx}` : `${groupStart}`
				}));
				displayNumber += group.messages.length;
				return result;
			});
		});

		const safeJsonParse = (str) => {
			try {
				return JSON.parse(str);
			} catch (e) {
				return { raw: str };
			}
		};

		const formatJson = (obj) => {
			try {
				return JSON.stringify(obj, null, 2);
			} catch (e) {
				return String(obj);
			}
		};

		const formatBytes = (bytes) => {
			if (bytes === 0) return '0 Bytes';
			const k = 1024;
			const sizes = ['Bytes', 'KB', 'MB'];
			const i = Math.floor(Math.log(bytes) / Math.log(k));
			return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
		};

		const highlightJson = (obj) => {
			const json = formatJson(obj);
			
			const escaped = json
				.replace(/&/g, '&amp;')
				.replace(/</g, '&lt;')
				.replace(/>/g, '&gt;');
			
			return escaped
				.replace(/"([^"]+)":/g, '<span class="json-key">"$1"</span>:')
				.replace(/: "([^"]*)"/g, ': <span class="json-string">"$1"</span>')
				.replace(/: (\d+\.?\d*)/g, ': <span class="json-number">$1</span>')
				.replace(/: (true|false)/g, ': <span class="json-boolean">$1</span>')
				.replace(/: (null)/g, ': <span class="json-null">$1</span>')
				.replace(/(\{|\}|\[|\])/g, '<span class="json-bracket">$1</span>');
		};

		const showAlert = (message, type = 'info') => {
			console.log(`[Alert ${type}]`, message);
			const alert = { message, type };
			alerts.value.push(alert);
			setTimeout(() => {
				const idx = alerts.value.indexOf(alert);
				if (idx > -1) alerts.value.splice(idx, 1);
			}, 2000);
		};

		const setEncryptionMode = async (mode) => {
			console.log('[Mode] Setting encryption mode to:', mode);
			try {
				const res = await fetch('/api/mode', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({ encryption_mode: mode })
				});
				const data = await res.json();
				if (data.status === 'ok') {
					currentMode.encryption = mode;
					console.log('[Mode] Encryption mode updated:', data);
					showAlert(`Encryption mode set to ${mode}`, 'success');
				} else {
					console.error('[Mode] Server rejected encryption mode:', data);
					showAlert(data.message, 'error');
				}
			} catch (e) {
				console.error('[Mode] Failed to set encryption mode:', e);
				showAlert(`Error: ${e.message}`, 'error');
			}
		};

		const setOperationMode = async (mode) => {
			console.log('[Mode] Setting operation mode to:', mode);
			try {
				const res = await fetch('/api/mode', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({ operation_mode: mode })
				});
				const data = await res.json();
				if (data.status === 'ok') {
					currentMode.operation = mode;
					console.log('[Mode] Operation mode updated:', data);
					showAlert(`Operation mode set to ${mode}`, 'success');
					
					if (mode === 'EDIT') {
						await refreshHeldMessages();
					} else {
						heldMessages.value = [];
					}
					
					selectedMessage.value = null;
				} else {
					console.error('[Mode] Server rejected operation mode:', data);
					showAlert(data.message, 'error');
				}
			} catch (e) {
				console.error('[Mode] Failed to set operation mode:', e);
				showAlert(`Error: ${e.message}`, 'error');
			}
		};

		const sendSelectedMessage = async () => {
			if (!selectedMessage.value) return;
			
			const idx = selectedMessage.value.originalIdx;
			console.log('[Editor] Sending message at index:', idx);
			
			try {
				const content = editedMessageText.value || selectedMessage.value.message;
				
				const res = await fetch('/api/held/send', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({ 
						index: idx,
						content: content
					})
				});
				
				const data = await res.json();
				console.log('[Editor] Send response:', data);
				
				if (data.status === 'ok') {
					showAlert('Message sent', 'success');
					await refreshHeldMessages();
					selectedMessage.value = null;
				} else {
					showAlert(data.message || 'Failed to send message', 'error');
				}
			} catch (e) {
				console.error('[Editor] Send failed:', e);
				showAlert(`Error: ${e.message}`, 'error');
			}
		};

		const sendAllHeldMessages = async () => {
			console.log('[Editor] Sending all held messages');
			
			try {
				const res = await fetch('/api/held/send-all', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' }
				});
				const data = await res.json();
				
				if (data.status === 'ok') {
					showAlert(data.message, 'success');
					await refreshHeldMessages();
					selectedMessage.value = null;
				} else {
					showAlert(data.message || 'Failed to send messages', 'error');
				}
			} catch (e) {
				console.error('[Editor] Failed to send all messages:', e);
				showAlert(`Error: ${e.message}`, 'error');
			}
		};

		const discardSelectedMessage = async () => {
			if (!selectedMessage.value) return;
			
			const idx = selectedMessage.value.originalIdx;
			console.log('[Editor] Discarding message at index:', idx);
			
			try {
				showAlert('Message discarded', 'success');
				await refreshHeldMessages();
				selectedMessage.value = null;
			} catch (e) {
				console.error('[Editor] Discard failed:', e);
				showAlert(`Error: ${e.message}`, 'error');
			}
		};

		const clearHeldMessages = async () => {
			try {
				await fetch('/api/held/clear', { method: 'POST' });
				await refreshHeldMessages();
				selectedMessage.value = null;
				showAlert('All held messages cleared', 'success');
			} catch (e) {
				console.error('[Held] Clear failed:', e);
				showAlert(`Error: ${e.message}`, 'error');
			}
		};

		const refreshHeldMessages = async () => {
			try {
				const res = await fetch('/api/held');
				const data = await res.json();
				heldMessages.value = data.messages.map(msg => ({ ...msg, isHeld: true }));
				console.log('[Held] Refreshed, count:', heldMessages.value.length);
			} catch (e) {
				console.error('[Held] Refresh failed:', e);
			}
		};

		const handleKeystoreFile = (e) => {
			tlsFiles.keystore = e.target.files[0];
			tlsStatus.value = null;
			console.log('[TLS] Keystore file selected:', tlsFiles.keystore?.name);
		};

		const handlePasswordFile = (e) => {
			tlsFiles.password = e.target.files[0];
			tlsStatus.value = null;
			console.log('[TLS] Password file selected:', tlsFiles.password?.name);
		};

		const loadTLS = async () => {
			if (!tlsFiles.keystore || !tlsFiles.password) {
				showAlert('Please select both keystore and password files', 'error');
				return;
			}

			console.log('[TLS] Loading TLS certificates...');
			try {
				const passText = await tlsFiles.password.text();
				console.log('[TLS] Password file read, length:', passText.length);

				const keystoreArrayBuffer = await tlsFiles.keystore.arrayBuffer();
				console.log('[TLS] Keystore file read, size:', keystoreArrayBuffer.byteLength, 'bytes');
				
				const keystoreBase64 = btoa(
					new Uint8Array(keystoreArrayBuffer)
						.reduce((data, byte) => data + String.fromCharCode(byte), '')
				);
				console.log('[TLS] Keystore encoded to base64, length:', keystoreBase64.length);

				const res = await fetch('/api/keystore/load-base64', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({
						keystore_base64: keystoreBase64,
						password: passText.trim()
					})
				});

				const data = await res.json();
				console.log('[TLS] Server response:', data);
				
				if (data.status === 'ok') {
					tlsStatus.value = { ok: true, message: 'TLS Loaded Successfully' };
					showAlert('TLS certificates loaded', 'success');
				} else {
					tlsStatus.value = { ok: false, message: data.message };
					showAlert(data.message, 'error');
				}
			} catch (e) {
				console.error('[TLS] Load failed:', e);
				tlsStatus.value = { ok: false, message: e.message };
				showAlert(`Error: ${e.message}`, 'error');
			}
		};

		const updateStatus = async () => {
			try {
				const res = await fetch('/api/status');
				const status = await res.json();
				connected.value = status.connected;
				messageCount.value = status.message_count;
				currentMode.encryption = status.encryption_mode.toUpperCase();
				currentMode.operation = status.operation_mode.toUpperCase();
			} catch (e) {
				console.error('[Status] Update failed:', e);
			}
		};

		const refreshMessages = async () => {
			try {
				const res = await fetch('/api/logs');
				const newMessages = await res.json();
				
				if (newMessages.length !== messages.value.length) {
					const diff = newMessages.length - messages.value.length;
					console.log('[Messages] Received', diff, 'new message(s), total:', newMessages.length);
				}
				messages.value = newMessages;

				if (currentMode.operation === 'EDIT') {
					await refreshHeldMessages();
				}

				stats.clientToServer = messages.value.filter(m => m.direction.includes('client')).length;
				stats.serverToClient = messages.value.filter(m => m.direction.includes('server')).length;
			} catch (e) {
				console.error('[Messages] Refresh failed:', e);
			}
		};

		const selectMessage = (msg) => {
			console.log('[Editor] Message selected:', msg);
			selectedMessage.value = {
				...msg,
				parsed: safeJsonParse(msg.message)
			};
			editedMessageText.value = formatJson(selectedMessage.value.parsed);
		};

		const resetEdit = () => {
			console.log('[Editor] Resetting edit');
			if (selectedMessage.value) {
				editedMessageText.value = formatJson(selectedMessage.value.parsed);
			}
		};

		const clearLogs = async () => {
			console.log('[Logs] Clearing all logs');
			try {
				await fetch('/api/logs', { method: 'POST' });
				messages.value = [];
				heldMessages.value = [];
				stats.clientToServer = 0;
				stats.serverToClient = 0;
				selectedMessage.value = null;
				showAlert('Logs cleared', 'success');
			} catch (e) {
				console.error('[Logs] Clear failed:', e);
				showAlert(`Error: ${e.message}`, 'error');
			}
		};

		setInterval(updateStatus, 5000);
		setInterval(refreshMessages, 1000);

		updateStatus();
		refreshMessages();
		
		console.log('[App] Middleman UI initialized');

		return {
			alerts,
			currentMode,
			messages,
			heldMessages,
			selectedMessage,
			messageFilter,
			directionFilter,
			connected,
			messageCount,
			stats,
			editedMessageText,
			tlsStatus,
			keystoreInput,
			passwordInput,
			tlsReady,
			displayedMessages,
			sessionDuration,
			showEncrypted,
			setEncryptionMode,
			setOperationMode,
			sendSelectedMessage,
			sendAllHeldMessages,
			discardSelectedMessage,
			clearHeldMessages,
			handleKeystoreFile,
			handlePasswordFile,
			loadTLS,
			selectMessage,
			resetEdit,
			clearLogs,
			formatJson,
			highlightJson,
			formatBytes,
			formatTime,
			getDisplayContent,
			displayMessagePreview
		};
	}
});

app.mount('#app');
