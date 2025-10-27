document.addEventListener('DOMContentLoaded', () => {
	const fileInput = document.getElementById('fileInput');
	const fileHint = document.getElementById('fileHint');
	const layoutPanel = document.getElementById('layoutPanel');

	if (layoutPanel) layoutPanel.classList.remove('visible');

	fileInput.addEventListener('change', async (event) => {
		const file = event.target.files[0];
		if (!file) return;

		fileInput.disabled = true;
		document.getElementById('toggleLayout').disabled = true;
		document.getElementById('runLayout').disabled = true;
		document.getElementById('runCise').disabled = true;
		fileHint.textContent = 'Reading file...';

		try {
			const text = await readFileAsText(file);
			fileHint.textContent = 'Parsing repository...';
			const graphData = await parseRepomix(text);
			fileHint.textContent = 'Building graph...';
			initGraph(graphData);
			fileHint.textContent = 'Graph loaded successfully';
		} catch (err) {
			console.error('Failed to process file:', err);
			fileHint.textContent = 'Failed to process file: ' + err.message;
		} finally {
			fileInput.disabled = false;
			document.getElementById('toggleLayout').disabled = false;
			document.getElementById('runLayout').disabled = false;
			document.getElementById('runCise').disabled = false;
		}
	});
});

function readFileAsText(file) {
	return new Promise((resolve, reject) => {
		const reader = new FileReader();
		reader.onload = () => resolve(reader.result);
		reader.onerror = () => reject(reader.error);
		reader.readAsText(file);
	});
}
