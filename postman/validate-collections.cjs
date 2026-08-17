const fs = require('fs');
const path = require('path');

const workspace = path.resolve(__dirname, '..');
const collectionsDir = path.join(__dirname, 'collections');
const coverage = JSON.parse(fs.readFileSync(path.join(__dirname, 'coverage-report.json'), 'utf8'));
const errors = [];

function javaFiles(root) {
  if (!fs.existsSync(root)) return [];
  return fs.readdirSync(root, { withFileTypes: true }).flatMap(entry => {
    const full = path.join(root, entry.name);
    if (entry.isDirectory()) return javaFiles(full);
    return entry.isFile() && entry.name.endsWith('.java') ? [full] : [];
  });
}

function sourceMappingCount(service) {
  const root = service.service === 'legacy-product-service'
    ? path.join(workspace, 'product-service')
    : path.join(workspace, 'services', service.service);
  let count = 0;
  for (const file of javaFiles(path.join(root, 'src', 'main', 'java'))) {
    const source = fs.readFileSync(file, 'utf8');
    if (!/@(?:RestController|Controller)\b/.test(source)) continue;
    count += (source.match(/@(?:[\w.]+\.)?(?:Get|Post|Put|Patch|Delete)Mapping\b/g) || []).length;
  }
  return count;
}

function walk(items, visit) {
  for (const item of items || []) {
    if (item.request) visit(item);
    walk(item.item, visit);
  }
}

for (const service of coverage) {
  const collectionFile = path.join(__dirname, service.collection);
  if (!fs.existsSync(collectionFile)) {
    errors.push(`${service.service}: missing ${service.collection}`);
    continue;
  }
  let collection;
  try {
    collection = JSON.parse(fs.readFileSync(collectionFile, 'utf8'));
  } catch (error) {
    errors.push(`${service.service}: invalid JSON (${error.message})`);
    continue;
  }
  if (collection.info?.schema !== 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json') {
    errors.push(`${service.service}: collection is not Postman Collection v2.1`);
  }
  const variables = new Set((collection.variable || []).map(variable => variable.key));
  const controllerFolders = (collection.item || []).filter(folder => folder.name !== 'Service Operations');
  const controllerRequestCount = controllerFolders.reduce((sum, folder) => sum + (folder.item || []).length, 0);
  if (controllerFolders.length !== service.controllerCount) {
    errors.push(`${service.service}: expected ${service.controllerCount} controller folders, found ${controllerFolders.length}`);
  }
  if (controllerRequestCount !== service.requestCount) {
    errors.push(`${service.service}: expected ${service.requestCount} controller requests, found ${controllerRequestCount}`);
  }
  const sourceCount = sourceMappingCount(service);
  if (sourceCount !== service.requestCount) {
    errors.push(`${service.service}: source has ${sourceCount} mappings but coverage report has ${service.requestCount}`);
  }
  walk(collection.item, item => {
    if (!item.request.method || !item.request.url) {
      errors.push(`${service.service}: ${item.name} is missing a method or URL`);
    }
    const serialized = JSON.stringify(item.request);
    for (const match of serialized.matchAll(/\{\{([^}{$]+)\}\}/g)) {
      if (!variables.has(match[1])) errors.push(`${service.service}: ${item.name} uses undefined variable ${match[1]}`);
    }
    if (item.request.body?.mode === 'raw') {
      try {
        JSON.parse(item.request.body.raw);
      } catch (error) {
        errors.push(`${service.service}: ${item.name} has invalid example JSON (${error.message})`);
      }
    }
  });
}

const expectedFiles = new Set(coverage.map(service => path.basename(service.collection)));
for (const file of fs.readdirSync(collectionsDir).filter(file => file.endsWith('.postman_collection.json'))) {
  if (!expectedFiles.has(file)) errors.push(`Untracked generated collection: ${file}`);
}

if (errors.length) {
  console.error(errors.map(error => `- ${error}`).join('\n'));
  process.exit(1);
}

const controllers = coverage.reduce((sum, service) => sum + service.controllerCount, 0);
const requests = coverage.reduce((sum, service) => sum + service.requestCount, 0);
console.log(`Validated ${coverage.length} Postman collections: ${controllers} controllers and ${requests} controller requests are covered.`);
