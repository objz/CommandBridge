const PALETTE = [
	'#60a5fa', '#34d399', '#f87171', '#fbbf24', '#a78bfa',
	'#fb923c', '#f472b6', '#2dd4bf', '#818cf8', '#facc15'
];

function initGraph(graphData) {
	if (window.currentCy) {
		try {
			window.currentCy.destroy();
		} catch (e) {
			console.warn('Failed to destroy previous graph instance', e);
		}
	}

	const nodes = graphData.nodes || [];
	const edges = graphData.edges || [];
	const metadata = graphData.metadata || {};

	const subprojects = metadata.subprojects && metadata.subprojects.length
		? metadata.subprojects
		: Array.from(new Set(nodes.map(n => n.data.subproject))).sort();

	const colorMap = {};
	subprojects.forEach((sp, idx) => {
		colorMap[sp] = PALETTE[idx % PALETTE.length];
	});

	const elements = [];
	nodes.forEach(node => {
		const colour = colorMap[node.data.subproject] || '#9ca3af';
		elements.push({
			data: {
				id: node.data.id,
				label: node.data.label,
				cluster: node.data.cluster,
				subproject: node.data.subproject,
				color: colour
			}
		});
	});

	edges.forEach(edge => {
		const eid = edge.data.id || `${edge.data.source}->${edge.data.target}`;
		elements.push({
			data: {
				id: eid,
				source: edge.data.source,
				target: edge.data.target
			}
		});
	});

	if (typeof window !== 'undefined' && window.cytoscapeCise) {
		try {
			window.cytoscapeCise(cytoscape);
		} catch (err) {
			try {
				cytoscape.use(window.cytoscapeCise);
			} catch (e) {
				console.warn('Unable to register CiSE extension:', e);
			}
		}
	}

	const cy = cytoscape({
		container: document.getElementById('cy'),
		elements: elements,
		wheelSensitivity: 0.3,
		style: [
			{
				selector: 'node',
				style: {
					'background-color': 'data(color)',
					'label': 'data(label)',
					'color': '#1f2937',
					'text-valign': 'center',
					'text-halign': 'center',
					'font-size': 10,
					'width': 40,
					'height': 40,
					'text-wrap': 'wrap',
					'text-max-width': 80,
					'text-outline-width': 1,
					'text-outline-color': '#ffffff'
				}
			},
			{
				selector: 'node.inactive',
				style: {
					'opacity': 0.4
				}
			},
			{
				selector: 'edge',
				style: {
					'line-color': '#9ca3af',
					'target-arrow-color': '#9ca3af',
					'target-arrow-shape': 'triangle',
					'curve-style': 'bezier',
					'width': 1
				}
			},
			{
				selector: 'edge.highlight-out',
				style: {
					'line-color': '#f87171',
					'target-arrow-color': '#f87171',
					'width': 2,
					'opacity': 1
				}
			},
			{
				selector: 'edge.highlight-in',
				style: {
					'line-color': '#34d399',
					'target-arrow-color': '#34d399',
					'width': 2,
					'opacity': 1
				}
			},
			{
				selector: 'edge.faded',
				style: {
					'opacity': 0.1
				}
			},
			{
				selector: 'node.faded',
				style: {
					'opacity': 0.3
				}
			}
		]
	});

	window.currentCy = cy;

	function populateHud() {
		const infoPanel = document.getElementById('infoPanel');
		const legendPanel = document.getElementById('legendPanel');
		const legendList = document.getElementById('legendList');
		const statsContainer = document.getElementById('statsContainer');

		if (!legendList || !statsContainer) return;

		if (infoPanel) infoPanel.classList.remove('hidden');
		if (legendPanel) legendPanel.classList.remove('hidden');

		legendList.innerHTML = '';
		subprojects.forEach((sp) => {
			const li = document.createElement('li');
			li.className = 'legend-item';

			const colorBox = document.createElement('span');
			colorBox.className = 'legend-color';
			colorBox.style.backgroundColor = colorMap[sp] || '#6b7280';

			const labelSpan = document.createElement('span');
			labelSpan.textContent = sp;

			li.appendChild(colorBox);
			li.appendChild(labelSpan);
			legendList.appendChild(li);
		});

		const numNodes = nodes.length;
		const numEdges = edges.length;
		const clusters = new Set(nodes.map(n => n.data.cluster));

		const stats = [
			{ label: 'Classes', value: numNodes },
			{ label: 'Dependencies', value: numEdges },
			{ label: 'Subprojects', value: subprojects.length },
			{ label: 'Clusters', value: clusters.size }
		];

		statsContainer.innerHTML = '';
		stats.forEach(stat => {
			const div = document.createElement('div');
			div.className = 'stat-item';
			div.innerHTML = `
        <div class="stat-label">${stat.label}</div>
        <div class="stat-value">${stat.value}</div>
      `;
			statsContainer.appendChild(div);
		});
	}

	function runLayout() {
		const sep = parseFloat(document.getElementById('nodeSeparation').value) || 20;
		const interCoef = parseFloat(document.getElementById('idealEdgeCoef').value) || 1.4;
		const ratioVal = parseFloat(document.getElementById('maxRatio').value) || 0.1;
		const allowInside = document.getElementById('allowInside').value === 'true';
		const randomise = document.getElementById('randomize').value === 'true';

		const nodesByCluster = {};
		cy.nodes().forEach(function(n) {
			const c = n.data('cluster');
			if (!nodesByCluster[c]) nodesByCluster[c] = [];
			nodesByCluster[c].push(n);
		});

		let clusterKeys = Object.keys(nodesByCluster);
		const clusterCount = clusterKeys.length;

		if (clusterCount === 0) {
			cy.fit();
			return;
		}

		if (randomise) {
			clusterKeys = clusterKeys.sort(() => Math.random() - 0.5);
		}

		const baseClusterRadius = 100;
		const clusterSpacing = (sep * interCoef) + (clusterCount * 5);
		const clusterRadius = baseClusterRadius + clusterSpacing;
		const clusterAngleStep = 2 * Math.PI / clusterCount;

		clusterKeys.forEach(function(cluster, i) {
			const angle = clusterAngleStep * i;
			const centerX = Math.cos(angle) * clusterRadius;
			const centerY = Math.sin(angle) * clusterRadius;

			let nodeList = nodesByCluster[cluster];
			if (randomise) {
				nodeList = nodeList.slice().sort(() => Math.random() - 0.5);
			}

			const count = nodeList.length;
			if (count === 0) return;

			const nodeAngleStep = 2 * Math.PI / count;
			const baseNodeRadius = 30 + sep * ratioVal;
			const nodeRadius = baseNodeRadius + (count * ratioVal);

			nodeList.forEach(function(node, j) {
				let rad = nodeRadius;
				if (allowInside && j % 2 === 1) {
					rad = nodeRadius * 0.6;
				}
				const nodeAngle = nodeAngleStep * j;
				const x = centerX + rad * Math.cos(nodeAngle);
				const y = centerY + rad * Math.sin(nodeAngle);
				node.position({ x: x, y: y });
			});
		});

		cy.resize();
		cy.fit();
		updateInactiveNodes();
	}

	function runCiseLayout() {
		runRingLayout();
	}

	function runRingLayout() {
		const sepInput = parseFloat(document.getElementById('nodeSeparation').value) || 20;
		const sep = Math.min(sepInput, 20) * 0.5;
		const interCoef = parseFloat(document.getElementById('idealEdgeCoef').value) || 1.4;
		let ratioVal = parseFloat(document.getElementById('maxRatio').value);
		if (isNaN(ratioVal)) ratioVal = 0.1;
		const allowInside = document.getElementById('allowInside').value === 'true';
		const randomise = document.getElementById('randomize').value === 'true';

		const nodesByCluster = {};
		cy.nodes().forEach(function(n) {
			const c = n.data('cluster');
			if (!nodesByCluster[c]) nodesByCluster[c] = [];
			nodesByCluster[c].push(n);
		});

		let clusterKeys = Object.keys(nodesByCluster);
		const clusterCount = clusterKeys.length;

		if (clusterCount === 0) {
			cy.fit();
			return;
		}

		if (randomise) {
			clusterKeys = clusterKeys.sort(() => Math.random() - 0.5);
		}

		const baseMainRadius = 150;
		const mainRadius = baseMainRadius + (clusterCount * (sep + interCoef * 5));
		const clusterAngleStep = (2 * Math.PI) / clusterCount;

		clusterKeys.forEach(function(cluster, i) {
			const angle = clusterAngleStep * i;
			const centerX = mainRadius * Math.cos(angle);
			const centerY = mainRadius * Math.sin(angle);

			let nodeList = nodesByCluster[cluster];
			if (randomise) {
				nodeList = nodeList.slice().sort(() => Math.random() - 0.5);
			}

			const count = nodeList.length;
			if (count === 0) return;

			const baseNodeRadius = 25 + sep;
			const nodeRadius = baseNodeRadius + (count * ratioVal * 3);
			const nodeAngleStep = (2 * Math.PI) / count;

			nodeList.forEach(function(node, j) {
				let rad = nodeRadius;
				if (allowInside && j % 2 === 1) {
					rad = nodeRadius * 0.6;
				}
				const nodeAngle = nodeAngleStep * j;
				const x = centerX + rad * Math.cos(nodeAngle);
				const y = centerY + rad * Math.sin(nodeAngle);
				node.position({ x: x, y: y });
			});
		});

		cy.resize();
		cy.fit();
		updateInactiveNodes();
	}

	function updateInactiveNodes() {
		cy.nodes().forEach(n => {
			const outEdges = n.outgoers('edge').length;
			const inEdges = n.incomers('edge').length;
			if (outEdges === 0 && inEdges === 0) {
				n.addClass('inactive');
			} else {
				n.removeClass('inactive');
			}
		});
	}

	function handleNodeTap(evt) {
		const node = evt.target;
		const id = node.data('id');
		const code = metadata.code_map && metadata.code_map[id] ? metadata.code_map[id] : 'Code not available.';
		const path = metadata.path_map && metadata.path_map[id] ? metadata.path_map[id] : '';

		document.getElementById('modalTitle').textContent = id;
		document.getElementById('modalPath').textContent = path;
		const codeEl = document.getElementById('modalContent');

		if (typeof Prism !== 'undefined' && Prism.languages && Prism.languages.java) {
			codeEl.innerHTML = Prism.highlight(code, Prism.languages.java, 'java');
		} else {
			codeEl.textContent = code;
		}

		const modal = document.getElementById('codeModal');
		modal.classList.remove('hidden');
		modal.classList.add('visible');
	}

	let currentHighlightedNode = null;

	function handleContextTap(event) {
		if (event.target === cy) {
			cy.edges().removeClass('highlight-out highlight-in faded');
			cy.nodes().removeClass('faded');
			currentHighlightedNode = null;
			return;
		}

		if (!event.target || !event.target.isNode()) return;

		const n = event.target;

		if (currentHighlightedNode && currentHighlightedNode.id() === n.id()) {
			cy.edges().removeClass('highlight-out highlight-in faded');
			cy.nodes().removeClass('faded');
			currentHighlightedNode = null;
			return;
		}

		currentHighlightedNode = n;
		cy.edges().removeClass('highlight-out highlight-in faded');
		cy.nodes().removeClass('faded');

		const outEdges = n.connectedEdges().filter(function(e) {
			return e.source().id() === n.id();
		});
		const inEdges = n.connectedEdges().filter(function(e) {
			return e.target().id() === n.id();
		});

		cy.edges().addClass('faded');
		cy.nodes().addClass('faded');
		n.removeClass('faded');

		outEdges.forEach(function(edge) {
			edge.removeClass('faded');
			edge.addClass('highlight-out');
			edge.target().removeClass('faded');
		});

		inEdges.forEach(function(edge) {
			edge.removeClass('faded');
			edge.addClass('highlight-in');
			edge.source().removeClass('faded');
		});
	}

	cy.on('tap', 'node', handleNodeTap);
	cy.on('cxttap', handleContextTap);

	document.getElementById('toggleLayout').onclick = function() {
		document.getElementById('layoutPanel').classList.toggle('visible');
	};

	document.getElementById('runLayout').onclick = function() {
		runLayout();
	};

	document.getElementById('runCise').onclick = function() {
		runCiseLayout();
	};

	document.getElementById('modalClose').onclick = function() {
		const modal = document.getElementById('codeModal');
		modal.classList.add('hidden');
		modal.classList.remove('visible');
	};

	document.getElementById('codeModal').onclick = function(e) {
		if (e.target === e.currentTarget) {
			const modal = document.getElementById('codeModal');
			modal.classList.add('hidden');
			modal.classList.remove('visible');
		}
	};

	populateHud();
	setTimeout(() => {
		runLayout();
		updateInactiveNodes();
	}, 50);
}

if (typeof window !== 'undefined') {
	window.initGraph = initGraph;
}
