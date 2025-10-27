async function parseRepomix(xmlString) {
	const nodes = [];
	const edges = [];
	const codeMap = {};
	const pathMap = {};
	const fqcnToId = {};
	const subprojectSet = new Set();

	let pos = 0;

	while (true) {
		const startIdx = xmlString.indexOf('<file path="', pos);
		if (startIdx === -1) break;

		const pathStart = startIdx + '<file path="'.length;
		const quoteEnd = xmlString.indexOf('"', pathStart);
		if (quoteEnd === -1) break;

		const path = xmlString.slice(pathStart, quoteEnd);
		const tagEnd = xmlString.indexOf('>', quoteEnd);
		if (tagEnd === -1) break;

		const contentStart = tagEnd + 1;
		const endTag = '</file>';
		const endIdx = xmlString.indexOf(endTag, contentStart);
		if (endIdx === -1) break;

		const content = xmlString.slice(contentStart, endIdx).trim();
		pos = endIdx + endTag.length;

		if (!path.endsWith('.java') && !path.endsWith('.kt')) continue;
		if (!content) continue;

		let pkg = '';
		const pkgMatch = content.match(/package\s+([\w\.]+)\s*;/);
		if (pkgMatch) pkg = pkgMatch[1];

		let className = '';
		const classMatch = content.match(/\b(public|private|protected)?\s*(?:abstract\s+)?(?:class|interface|enum|record)\s+([A-Z][A-Za-z0-9_]*)/);
		if (classMatch) {
			className = classMatch[2];
		} else {
			continue;
		}

		const fqcn = pkg ? `${pkg}.${className}` : className;

		const pathParts = path.split('/');
		let subproject = pathParts.length > 0 ? pathParts[0] : 'unknown';
		if (subproject === 'src' || subproject === 'main') {
			if (pathParts.length > 1) subproject = pathParts[1];
		}
		subprojectSet.add(subproject);

		let cluster = '';
		if (pkg.startsWith('dev.objz.commandbridge.')) {
			const remainder = pkg.replace('dev.objz.commandbridge.', '');
			const segs = remainder.split('.');
			if (segs.length >= 2) {
				cluster = `${segs[0]}/${segs[1]}`;
			} else if (segs.length === 1) {
				cluster = segs[0];
			} else {
				cluster = subproject;
			}
		} else if (pkg) {
			const segs = pkg.split('.');
			if (segs.length >= 2) {
				cluster = `${segs[0]}/${segs[1]}`;
			} else {
				cluster = segs[0];
			}
		} else {
			cluster = subproject;
		}

		const nodeData = {
			id: fqcn,
			label: className,
			subproject: subproject,
			cluster: cluster
		};

		nodes.push({ data: nodeData });
		codeMap[fqcn] = content;
		pathMap[fqcn] = path;
		fqcnToId[fqcn] = fqcn;
	}

	for (const node of nodes) {
		const id = node.data.id;
		const sourceCode = codeMap[id];
		if (!sourceCode) continue;

		const importRegex = /import\s+((?:dev\.objz\.commandbridge)[\.\w]+)\s*;/g;
		let m;
		while ((m = importRegex.exec(sourceCode)) !== null) {
			const imported = m[1];
			if (imported.endsWith('.*')) continue;
			if (fqcnToId[imported] && imported !== id) {
				const sourceId = imported;
				const targetId = id;
				edges.push({
					data: {
						id: `${sourceId}->${targetId}`,
						source: sourceId,
						target: targetId
					}
				});
			}
		}
	}

	const metadata = {
		code_map: codeMap,
		path_map: pathMap,
		subprojects: Array.from(subprojectSet).sort()
	};

	return { nodes, edges, metadata };
}

if (typeof window !== 'undefined') {
	window.parseRepomix = parseRepomix;
}
