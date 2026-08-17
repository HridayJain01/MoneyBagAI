const fs = require('fs');
const path = require('path');

const workspace = path.resolve(__dirname, '..');
const outputDir = path.join(__dirname, 'collections');

const services = [
  { slug: 'account-service', name: 'Account Service', root: 'services/account-service', port: 8083 },
  { slug: 'api-gateway', name: 'API Gateway', root: 'services/api-gateway', port: 8090 },
  { slug: 'audit-service', name: 'Audit Service', root: 'services/audit-service', port: 8091 },
  { slug: 'branch-employee-service', name: 'Branch Employee Service', root: 'services/branch-employee-service', port: 8081 },
  { slug: 'configuration-service', name: 'Configuration Service', root: 'services/configuration-service', port: 8092 },
  { slug: 'customer-service', name: 'Customer Service', root: 'services/customer-service', port: 8082 },
  { slug: 'eureka-server', name: 'Eureka Server', root: 'services/eureka-server', port: 8080 },
  { slug: 'identity-service', name: 'Identity Service', root: 'services/identity-service', port: 8087 },
  { slug: 'ledger-service', name: 'Ledger Service', root: 'services/ledger-service', port: 8085 },
  { slug: 'notification-service', name: 'Notification Service', root: 'services/notification-service', port: 8089 },
  { slug: 'product-service', name: 'Product Service', root: 'services/product-service', port: 8088 },
  { slug: 'statement-reporting-service', name: 'Statement Reporting Service', root: 'services/statement-reporting-service', port: 8086 },
  { slug: 'transaction-service', name: 'Transaction Service', root: 'services/transaction-service', port: 8084 },
  { slug: 'legacy-product-service', name: 'Legacy Product Service', root: 'product-service', port: 8083 }
];

const variableExamples = {
  account: 'a0000000-0000-0000-0000-000000000101',
  accountHolderId: 'CIF900101',
  accountId: 'a0000000-0000-0000-0000-000000000101',
  accountNumber: '510000000101',
  actionType: 'ACCOUNT_OPENING',
  aggregateId: 'a0000000-0000-0000-0000-000000000101',
  aggregateType: 'ACCOUNT',
  applicationId: 'APP-000001',
  actorEmployeeId: '101',
  amount: '100.00',
  branch: 'BR001',
  branchCode: 'BR001',
  branchId: 'BR001',
  cardId: 'card-000001',
  channel: 'NEFT',
  cif: 'CIF900101',
  cifNo: 'CIF900101',
  code: '110100',
  configKey: 'business-date',
  correlationId: '{{$guid}}',
  createdBy: '101',
  customerAccountId: 'a0000000-0000-0000-0000-000000000101',
  customerId: 'CIF900101',
  date: '2026-08-17',
  documentId: '1',
  empId: '1002',
  employeeId: '1004',
  eventId: '{{$guid}}',
  eventType: 'ACCOUNT_UPDATED',
  fileId: 'file-000001',
  fiscalYear: '2026',
  flagKey: 'instant-payments',
  from: '2026-08-01',
  holdId: 'hold-000001',
  holidayId: '1',
  id: '1',
  ifsc: 'MBAG0000001',
  ifscCode: 'MBAG0000001',
  journalId: '1',
  limitType: 'PER_TRANSACTION',
  maxAmount: '100000.00',
  minAmount: '0.01',
  name: 'Asha',
  namespace: 'banking',
  notificationId: 'notification-000001',
  productCategory: 'DEPOSIT',
  productCode: 'SAV-REG',
  productType: 'SAVINGS',
  productVersionId: '1',
  reference: 'JE-POSTMAN-001',
  recipient: 'asha@example.com',
  requestId: 'request-000001',
  rail: 'INTERNAL',
  roleId: '1',
  ruleId: '1',
  scheduleId: 'schedule-000001',
  search: 'admin',
  sessionId: '',
  size: '25',
  sourceService: 'account-service',
  status: 'ACTIVE',
  to: '2026-08-17',
  token: 'download-token',
  transactionId: 'tx-000001',
  transactionType: 'DEPOSIT',
  query: 'Asha',
  userId: '4',
  versionNumber: '1'
};

const fieldStringExamples = {
  accountHolderId: 'CIF900101',
  accountId: variableExamples.accountId,
  accountName: 'Primary Savings Account',
  accountNumber: variableExamples.accountNumber,
  maskedAccountNumber: 'XXXXXXXX0001',
  actionType: 'ACCOUNT_OPENING',
  actorEmployeeId: '101',
  addressLine1: '10 Park Road',
  addressType: 'RESIDENTIAL',
  aggregateId: variableExamples.accountId,
  aggregateType: 'ACCOUNT',
  bodyTemplate: 'Hello {{name}}, your request is complete.',
  branchCode: 'BR001',
  branchId: 'BR001',
  channel: 'EMAIL',
  cif: variableExamples.cif,
  cifNo: variableExamples.cifNo,
  city: 'Mumbai',
  country: 'India',
  currency: 'INR',
  currencyCode: 'INR',
  customerId: variableExamples.cifNo,
  description: 'Postman example request',
  direction: 'DEBIT',
  docNumber: 'ABCDE1234F',
  docType: 'PAN',
  documentNumber: 'ABCDE1234F',
  email: 'asha@example.com',
  eventId: '{{$guid}}',
  eventType: 'ACCOUNT_UPDATED',
  fileUrl: 'demo://kyc/pan',
  filePath: 'demo://kyc/pan',
  firstName: 'Asha',
  frequency: 'MONTHLY',
  gender: 'FEMALE',
  holderRole: 'JOINT',
  holdType: 'MANUAL',
  ifsc: 'MBAG0000001',
  journalReference: 'JE-POSTMAN-001',
  lastName: 'Sharma',
  line1: '10 Park Road',
  limitType: 'PER_TRANSACTION',
  locale: 'en-IN',
  mobile: '9876543210',
  narration: 'Postman example transaction',
  namespace: 'banking',
  notes: 'Resolved from Postman',
  outputFormat: 'PDF',
  panNo: 'ABCDE1234F',
  panNumber: 'ABCDE1234F',
  password: 'Password@123',
  permissions: 'TRANSACTION_CREATE,TRANSACTION_VIEW,RECONCILIATION_MANAGE',
  phoneNumber: '9876543210',
  pincode: '400001',
  postalCode: '400001',
  productCategory: 'DEPOSIT',
  productCode: variableExamples.productCode,
  productName: 'Postman Savings Product',
  productType: 'SAVINGS',
  reason: 'Postman example',
  recipient: 'asha@example.com',
  reference: 'POSTMAN-REF-001',
  remarks: 'Approved from Postman',
  reportType: 'ACCOUNT_STATEMENT',
  resolution: 'RESOLVED',
  roleNames: 'OPERATIONS,MANAGER',
  roles: 'OPERATIONS',
  side: 'DEBIT',
  sourceEventId: '{{$guid}}',
  sourceService: 'postman',
  state: 'Maharashtra',
  status: 'ACTIVE',
  subjectTemplate: 'Moneybags notification',
  templateCode: 'POSTMAN_TEMPLATE',
  transactionId: variableExamples.transactionId,
  transactionReference: 'TXN-POSTMAN-001',
  transactionType: 'DEPOSIT',
  type: 'SAVINGS',
  username: 'opsadmin'
};

function javaFiles(root) {
  if (!fs.existsSync(root)) return [];
  const found = [];
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const full = path.join(root, entry.name);
    if (entry.isDirectory()) found.push(...javaFiles(full));
    else if (entry.isFile() && entry.name.endsWith('.java')) found.push(full);
  }
  return found;
}

function matching(source, openIndex, openChar = '(', closeChar = ')') {
  let depth = 0;
  let quote = null;
  let lineComment = false;
  let blockComment = false;
  for (let i = openIndex; i < source.length; i += 1) {
    const ch = source[i];
    const next = source[i + 1];
    if (lineComment) {
      if (ch === '\n') lineComment = false;
      continue;
    }
    if (blockComment) {
      if (ch === '*' && next === '/') { blockComment = false; i += 1; }
      continue;
    }
    if (!quote && ch === '/' && next === '/') { lineComment = true; i += 1; continue; }
    if (!quote && ch === '/' && next === '*') { blockComment = true; i += 1; continue; }
    if (quote) {
      if (ch === '\\') { i += 1; continue; }
      if (ch === quote) quote = null;
      continue;
    }
    if (ch === '"' || ch === "'") { quote = ch; continue; }
    if (ch === openChar) depth += 1;
    else if (ch === closeChar) {
      depth -= 1;
      if (depth === 0) return i;
    }
  }
  return -1;
}

function annotationEnd(source, atIndex) {
  let cursor = atIndex + 1;
  while (cursor < source.length && /[\w.]/.test(source[cursor])) cursor += 1;
  while (/\s/.test(source[cursor] || '')) cursor += 1;
  return source[cursor] === '(' ? matching(source, cursor) + 1 : cursor;
}

function annotationPath(annotationText) {
  const match = annotationText.match(/"([^"\\]*(?:\\.[^"\\]*)*)"/);
  return match ? match[1] : '';
}

function requestMappingPath(annotationText) {
  const match = annotationText.match(/@(?:[\w.]+\.)?RequestMapping\s*\(/);
  if (!match) return '';
  const start = match.index + match[0].length - 1;
  const end = matching(annotationText, start);
  return annotationPath(annotationText.slice(start, end + 1));
}

function joinUrlPath(base, child) {
  const joined = `${base || ''}/${child || ''}`.replace(/\/{2,}/g, '/');
  if (!joined || joined === '/') return '/';
  return joined.startsWith('/') ? joined.replace(/\/$/, '') : `/${joined.replace(/\/$/, '')}`;
}

function skipWhitespaceAndComments(source, start) {
  let cursor = start;
  while (cursor < source.length) {
    if (/\s/.test(source[cursor])) { cursor += 1; continue; }
    if (source.startsWith('//', cursor)) {
      const newline = source.indexOf('\n', cursor + 2);
      cursor = newline < 0 ? source.length : newline + 1;
      continue;
    }
    if (source.startsWith('/*', cursor)) {
      const end = source.indexOf('*/', cursor + 2);
      cursor = end < 0 ? source.length : end + 2;
      continue;
    }
    break;
  }
  return cursor;
}

function methodAfterMapping(source, mappingEnd) {
  let cursor = skipWhitespaceAndComments(source, mappingEnd);
  while (source[cursor] === '@') {
    cursor = annotationEnd(source, cursor);
    cursor = skipWhitespaceAndComments(source, cursor);
  }
  const paramsOpen = source.indexOf('(', cursor);
  if (paramsOpen < 0) return null;
  const declaration = source.slice(cursor, paramsOpen).trim();
  const nameMatch = declaration.match(/([A-Za-z_$][\w$]*)\s*$/);
  if (!nameMatch) return null;
  const paramsClose = matching(source, paramsOpen);
  if (paramsClose < 0) return null;
  return {
    name: nameMatch[1],
    signature: source.slice(paramsOpen + 1, paramsClose),
    end: paramsClose + 1
  };
}

function splitTopLevel(text, delimiter = ',') {
  const parts = [];
  let start = 0;
  let round = 0;
  let angle = 0;
  let square = 0;
  let quote = null;
  for (let i = 0; i < text.length; i += 1) {
    const ch = text[i];
    if (quote) {
      if (ch === '\\') i += 1;
      else if (ch === quote) quote = null;
      continue;
    }
    if (ch === '"' || ch === "'") quote = ch;
    else if (ch === '(') round += 1;
    else if (ch === ')') round -= 1;
    else if (ch === '<') angle += 1;
    else if (ch === '>') angle -= 1;
    else if (ch === '[') square += 1;
    else if (ch === ']') square -= 1;
    else if (ch === delimiter && round === 0 && angle === 0 && square === 0) {
      parts.push(text.slice(start, i).trim());
      start = i + 1;
    }
  }
  const tail = text.slice(start).trim();
  if (tail) parts.push(tail);
  return parts;
}

function stripAnnotations(parameter) {
  let result = '';
  for (let cursor = 0; cursor < parameter.length;) {
    if (parameter[cursor] !== '@') { result += parameter[cursor]; cursor += 1; continue; }
    cursor = annotationEnd(parameter, cursor);
  }
  return result.replace(/\bfinal\b/g, '').replace(/\s+/g, ' ').trim();
}

function parameterName(parameter) {
  const clean = stripAnnotations(parameter);
  const match = clean.match(/([A-Za-z_$][\w$]*)\s*$/);
  return match ? match[1] : null;
}

function parameterType(parameter) {
  const clean = stripAnnotations(parameter);
  const match = clean.match(/^(.+?)\s+[A-Za-z_$][\w$]*$/);
  return match ? match[1].trim() : null;
}

function annotationOptions(parameter, annotationName) {
  const regex = new RegExp(`@(?:[\\w.]+\\.)?${annotationName}\\b`);
  const found = regex.exec(parameter);
  if (!found) return null;
  const cursor = found.index + found[0].length;
  if (parameter[cursor] !== '(') return '';
  const end = matching(parameter, cursor);
  return end < 0 ? '' : parameter.slice(cursor + 1, end);
}

function namedAnnotationValue(options) {
  if (!options) return null;
  const named = options.match(/(?:value|name)\s*=\s*"([^"]+)"/);
  if (named) return named[1];
  const positional = options.match(/^\s*"([^"]+)"/);
  return positional ? positional[1] : null;
}

function parseParameters(signature) {
  const result = { query: [], headers: [], bodyType: null };
  for (const parameter of splitTopLevel(signature)) {
    const name = parameterName(parameter);
    if (!name) continue;
    const requestParam = annotationOptions(parameter, 'RequestParam');
    if (requestParam !== null) {
      const key = namedAnnotationValue(requestParam) || name;
      const defaultMatch = requestParam.match(/defaultValue\s*=\s*"([^"]+)"/);
      result.query.push({ key, value: defaultMatch ? defaultMatch[1] : exampleVariable(key) });
    }
    const requestHeader = annotationOptions(parameter, 'RequestHeader');
    if (requestHeader !== null) {
      const key = namedAnnotationValue(requestHeader) || name;
      result.headers.push({ key, value: headerExample(key) });
    }
    if (annotationOptions(parameter, 'RequestBody') !== null) {
      result.bodyType = parameterType(parameter);
    }
    if (/\bPageable\b/.test(parameter)) {
      result.query.push({ key: 'page', value: '0' }, { key: 'size', value: '25' });
    }
  }
  return result;
}

function parseControllers(serviceRoot) {
  const sourceRoot = path.join(serviceRoot, 'src', 'main', 'java');
  const controllers = [];
  for (const file of javaFiles(sourceRoot)) {
    const source = fs.readFileSync(file, 'utf8');
    if (!/@(?:RestController|Controller)\b/.test(source)) continue;
    const classRegex = /(?:public\s+)?(?:final\s+)?class\s+([A-Za-z_$][\w$]*)\b/g;
    let classMatch;
    while ((classMatch = classRegex.exec(source)) !== null) {
      const className = classMatch[1];
      const previousBrace = source.lastIndexOf('}', classMatch.index);
      const previousSemicolon = source.lastIndexOf(';', classMatch.index);
      const prefixStart = Math.max(previousBrace, previousSemicolon) + 1;
      const annotations = source.slice(prefixStart, classMatch.index);
      if (!/@(?:RestController|Controller)\b/.test(annotations)) continue;
      const classOpen = source.indexOf('{', classRegex.lastIndex);
      const classClose = matching(source, classOpen, '{', '}');
      if (classOpen < 0 || classClose < 0) continue;
      const classSource = source.slice(classOpen + 1, classClose);
      const basePath = requestMappingPath(annotations);
      const methods = [];
      const mappingRegex = /@(?:[\w.]+\.)?(Get|Post|Put|Patch|Delete)Mapping\b/g;
      let mappingMatch;
      while ((mappingMatch = mappingRegex.exec(classSource)) !== null) {
        const end = annotationEnd(classSource, mappingMatch.index);
        const annotation = classSource.slice(mappingMatch.index, end);
        const method = methodAfterMapping(classSource, end);
        if (!method) continue;
        const params = parseParameters(method.signature);
        methods.push({
          httpMethod: mappingMatch[1].toUpperCase(),
          methodName: method.name,
          path: joinUrlPath(basePath, annotationPath(annotation)),
          ...params
        });
        mappingRegex.lastIndex = method.end;
      }
      if (methods.length) {
        controllers.push({
          name: className,
          source: path.relative(workspace, file).replace(/\\/g, '/'),
          methods
        });
      }
      classRegex.lastIndex = classClose + 1;
    }
  }
  return controllers.sort((a, b) => a.name.localeCompare(b.name));
}

function buildTypeIndex(serviceRoot) {
  const index = { records: new Map(), classes: new Map(), enums: new Map() };
  const sourceRoot = path.join(serviceRoot, 'src', 'main', 'java');
  for (const file of javaFiles(sourceRoot)) {
    const source = fs.readFileSync(file, 'utf8');
    const recordRegex = /\brecord\s+([A-Za-z_$][\w$]*)\s*\(/g;
    let match;
    while ((match = recordRegex.exec(source)) !== null) {
      const open = source.indexOf('(', match.index);
      const close = matching(source, open);
      if (close > open) index.records.set(match[1], source.slice(open + 1, close));
      recordRegex.lastIndex = close > open ? close + 1 : recordRegex.lastIndex;
    }
    const enumRegex = /\benum\s+([A-Za-z_$][\w$]*)\s*\{/g;
    while ((match = enumRegex.exec(source)) !== null) {
      const open = source.indexOf('{', match.index);
      const close = matching(source, open, '{', '}');
      if (close < 0) continue;
      const initial = source.slice(open + 1, close).split(';')[0];
      const values = splitTopLevel(initial).map(value => value.trim().match(/^([A-Z][A-Z0-9_]*)/)?.[1]).filter(Boolean);
      if (values.length) index.enums.set(match[1], values);
      enumRegex.lastIndex = close + 1;
    }
    const classRegex = /\bclass\s+([A-Za-z_$][\w$]*)\s*[^\{]*\{/g;
    while ((match = classRegex.exec(source)) !== null) {
      const open = source.indexOf('{', match.index);
      const close = matching(source, open, '{', '}');
      if (close < 0) continue;
      const block = source.slice(open + 1, close);
      const fields = [];
      const fieldRegex = /\bprivate\s+(?:final\s+)?([A-Za-z_$][\w$]*(?:\s*<[^;=]+>)?(?:\[\])?)\s+([A-Za-z_$][\w$]*)\s*(?:=[^;]*)?;/g;
      let field;
      while ((field = fieldRegex.exec(block)) !== null) fields.push({ type: field[1].replace(/\s+/g, ''), name: field[2] });
      if (fields.length) index.classes.set(match[1], fields);
      classRegex.lastIndex = close + 1;
    }
  }
  return index;
}

function simpleType(type) {
  return (type || '').replace(/^.*\./, '').replace(/\?\s*(?:extends|super)\s+/, '').trim();
}

function exampleForField(type, name, index, depth, seen) {
  const normalizedType = simpleType((type || '').replace(/\s+/g, ''));
  if (index.enums.has(normalizedType)) {
    const preferredEnums = {
      DocumentVerifyStatus: 'VERIFIED',
      KycStatus: 'PENDING',
      CustomerStatus: 'ACTIVE',
      ProductStatus: 'ACTIVE'
    };
    return preferredEnums[normalizedType] || index.enums.get(normalizedType)[0];
  }
  if (/^(String|UUID|Character|char)$/.test(normalizedType)) {
    if (fieldStringExamples[name] !== undefined) return fieldStringExamples[name];
    if (/date$/i.test(name)) return '2026-08-17';
    return 'string';
  }
  if (normalizedType === 'LocalDate') return '2026-08-17';
  if (normalizedType === 'LocalTime') return /close|end/i.test(name) ? '17:00:00' : '09:00:00';
  if (/^(Instant|LocalDateTime|OffsetDateTime|ZonedDateTime)$/.test(normalizedType)) return '2026-08-17T10:00:00Z';
  if (/^(Boolean|boolean)$/.test(normalizedType)) return true;
  if (/^(BigDecimal|Double|Float|double|float)$/.test(normalizedType)) return 100.00;
  if (/^(Long|Integer|Short|Byte|long|int|short|byte)$/.test(normalizedType)) return 1;
  return exampleForType(type, index, depth, seen, name);
}

function exampleForType(rawType, index, depth = 0, seen = new Set(), fieldName = '') {
  if (!rawType || depth > 4) return {};
  const type = rawType.replace(/\s+/g, '');
  const list = type.match(/^(?:List|Set|Collection)<(.+)>$/);
  if (list) return [exampleForType(list[1], index, depth + 1, seen, fieldName)];
  const map = type.match(/^Map<.+>$/);
  if (map) return { key: 'value' };
  const unqualified = simpleType(type);
  if (/^(String|UUID|Character|char)$/.test(unqualified)) return fieldStringExamples[fieldName] ?? 'string';
  if (/^(Long|Integer|Short|Byte|long|int|short|byte)$/.test(unqualified)) return 1;
  if (/^(BigDecimal|Double|Float|double|float)$/.test(unqualified)) return 100.00;
  if (/^(Boolean|boolean)$/.test(unqualified)) return true;
  if (unqualified === 'LocalDate') return '2026-08-17';
  if (unqualified === 'LocalTime') return '09:00:00';
  if (/^(Instant|LocalDateTime|OffsetDateTime|ZonedDateTime)$/.test(unqualified)) return '2026-08-17T10:00:00Z';
  if (index.enums.has(unqualified)) return index.enums.get(unqualified)[0];
  if (seen.has(unqualified)) return {};
  const nextSeen = new Set(seen).add(unqualified);
  if (index.records.has(unqualified)) {
    const object = {};
    for (const parameter of splitTopLevel(index.records.get(unqualified))) {
      const name = parameterName(parameter);
      const parameterTypeName = parameterType(parameter);
      if (name && parameterTypeName) object[name] = exampleForField(parameterTypeName, name, index, depth + 1, nextSeen);
    }
    return object;
  }
  if (index.classes.has(unqualified)) {
    const object = {};
    for (const field of index.classes.get(unqualified)) object[field.name] = exampleForField(field.type, field.name, index, depth + 1, nextSeen);
    return object;
  }
  return {};
}

function exampleVariable(name) {
  return variableExamples[name] ?? (name.toLowerCase().includes('date') ? '2026-08-17' : `{{${name}}}`);
}

function headerExample(name) {
  const normalized = name.toLowerCase();
  if (normalized === 'idempotency-key') return '{{idempotencyKey}}';
  if (normalized === 'x-service-name') return '{{serviceName}}';
  if (normalized === 'x-session-id') return '{{sessionId}}';
  return `{{${name.replace(/-([a-z])/g, (_, letter) => letter.toUpperCase())}}}`;
}

function titleCase(value) {
  return value.replace(/([a-z0-9])([A-Z])/g, '$1 $2').replace(/[_-]+/g, ' ').replace(/\b\w/g, letter => letter.toUpperCase());
}

function postmanPath(routePath) {
  return routePath.replace(/\{([A-Za-z_$][\w$]*)\}/g, '{{$1}}');
}

function requestItem(method, typeIndex, service) {
  const query = method.query.map(({ key, value }) => `${encodeURIComponent(key)}=${encodeURIComponent(value).replace(/%7B%7B/g, '{{').replace(/%7D%7D/g, '}}')}`);
  const route = postmanPath(method.path);
  const rawUrl = `{{baseUrl}}${route}${query.length ? `?${query.join('&')}` : ''}`;
  const headers = [...method.headers];
  if (service.slug === 'statement-reporting-service' && method.methodName === 'account') {
    headers.filter(header => header.key.toLowerCase() === 'x-service-name').forEach(header => { header.value = 'account-service'; });
  }
  if (service.slug === 'statement-reporting-service' && method.methodName === 'transaction') {
    headers.filter(header => header.key.toLowerCase() === 'x-service-name').forEach(header => { header.value = 'transaction-service'; });
  }
  if (method.bodyType) headers.unshift({ key: 'Content-Type', value: 'application/json' });
  if (method.path.startsWith('/api/') && ['account-service', 'transaction-service'].includes(service.slug)) {
    headers.push(
      { key: 'X-Employee-Id', value: '{{employeeId}}', description: 'Direct calls only; the gateway injects this from sessionId.' },
      { key: 'X-Branch-Code', value: '{{branchCode}}', description: 'Direct calls only; the gateway injects this from sessionId.' },
      { key: 'X-Permissions', value: '{{permissions}}', description: 'Direct calls only; the gateway injects this from sessionId.' },
      { key: 'X-Correlation-Id', value: '{{correlationId}}' }
    );
  }
  if (method.path.startsWith('/api/') && service.slug === 'statement-reporting-service') {
    headers.push(
      { key: 'X-User-Id', value: '{{userId}}', description: 'Direct calls only; the gateway injects this from sessionId.' },
      { key: 'X-Customer-Id', value: '{{customerId}}', description: 'Direct customer scope; staff requests may leave this blank.' },
      { key: 'X-Employee-Id', value: '{{employeeId}}', description: 'Direct calls only; the gateway injects this from sessionId.' },
      { key: 'X-Branch-Id', value: '{{branchId}}', description: 'Direct calls only; the gateway injects this from sessionId.' },
      { key: 'X-Permissions', value: '{{permissions}}', description: 'Direct calls only; the gateway injects this from sessionId.' },
      { key: 'X-Correlation-Id', value: '{{correlationId}}' }
    );
  }
  const request = {
    method: method.httpMethod,
    header: deduplicateHeaders(headers),
    url: rawUrl,
    description: `Generated from ${method.source || 'the Spring controller'}.${method.methodName}${method.bodyType ? `\n\nRequest body type: ${method.bodyType}` : ''}`
  };
  if (method.bodyType) {
    request.body = {
      mode: 'raw',
      raw: JSON.stringify(exampleForType(method.bodyType, typeIndex), null, 2),
      options: { raw: { language: 'json' } }
    };
  }
  const item = { name: `${method.httpMethod} ${titleCase(method.methodName)}`, request, response: [] };
  if (method.methodName === 'login') {
    item.event = [{
      listen: 'test',
      script: {
        type: 'text/javascript',
        exec: [
          'if (pm.response.code >= 200 && pm.response.code < 300) {',
          '  const data = pm.response.json();',
          '  const sessionId = data.sessionId || data.token || data.accessToken;',
          '  if (sessionId) pm.collectionVariables.set("sessionId", sessionId);',
          '}'
        ]
      }
    }];
  }
  return item;
}

function deduplicateHeaders(headers) {
  const seen = new Set();
  return headers.filter(header => {
    const key = header.key.toLowerCase();
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function operationalFolder(service) {
  const items = [
    simpleRequest('GET Health', 'GET', '{{baseUrl}}/actuator/health'),
    simpleRequest('GET Generated OpenAPI', 'GET', '{{baseUrl}}/api-docs')
  ];
  if (service.slug === 'eureka-server') {
    items.push(simpleRequest('GET Eureka Dashboard', 'GET', '{{baseUrl}}/'));
    items.push({
      name: 'GET Registered Applications',
      request: { method: 'GET', header: [{ key: 'Accept', value: 'application/json' }], url: '{{baseUrl}}/eureka/apps' },
      response: []
    });
  }
  return { name: 'Service Operations', item: items };
}

function simpleRequest(name, method, url) {
  return { name, request: { method, header: [], url }, response: [] };
}

function collectionFor(service, controllers, typeIndex) {
  for (const controller of controllers) {
    for (const method of controller.methods) method.source = controller.source;
  }
  const collection = {
    info: {
      _postman_id: deterministicId(service.slug),
      name: `Moneybags - ${service.name}`,
      description: `Generated from the current Spring controller source for ${service.root}. Requests default to direct service access on port ${service.port}. Set baseUrl to http://localhost:8090 to exercise public routes through the API gateway. Internal /internal routes are intentionally called directly.`,
      schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'
    },
    auth: { type: 'bearer', bearer: [{ key: 'token', value: '{{sessionId}}', type: 'string' }] },
    event: [{
      listen: 'prerequest',
      script: {
        type: 'text/javascript',
        exec: [
          'if (!pm.collectionVariables.get("idempotencyKey")) {',
          '  pm.collectionVariables.set("idempotencyKey", pm.variables.replaceIn("{{$guid}}"));',
          '}',
          'pm.collectionVariables.set("correlationId", pm.variables.replaceIn("{{$guid}}"));'
        ]
      }
    }],
    variable: baseVariables(service),
    item: [
      operationalFolder(service),
      ...controllers.map(controller => ({
        name: controller.name,
        description: `Source: ${controller.source}`,
        item: controller.methods.map(method => requestItem(method, typeIndex, service))
      }))
    ]
  };
  return collection;
}

function deterministicId(value) {
  let hash = 2166136261;
  for (const char of `moneybags-${value}`) {
    hash ^= char.charCodeAt(0);
    hash = Math.imul(hash, 16777619);
  }
  const hex = (hash >>> 0).toString(16).padStart(8, '0');
  return `${hex}-0000-4000-8000-${hex}${hex.slice(0, 4)}`;
}

function baseVariables(service) {
  return [
    { key: 'baseUrl', value: `http://localhost:${service.port}`, type: 'string' },
    { key: 'sessionId', value: '', type: 'string' },
    { key: 'employeeId', value: '1004', type: 'string' },
    { key: 'branchCode', value: 'BR001', type: 'string' },
    { key: 'permissions', value: 'TRANSACTION_CREATE,TRANSACTION_APPROVE,TRANSACTION_CANCEL,TRANSACTION_CANCEL_ANY,TRANSACTION_REVERSE,TRANSACTION_VIEW,TRANSACTION_VIEW_ALL_BRANCHES,RECONCILIATION_MANAGE,STATEMENT_VIEW,REPORT_VIEW,REPORT_ADMIN,ACCOUNT_VIEW,ACCOUNT_VIEW_ALL_BRANCHES,ACCOUNT_OPEN,ACCOUNT_APPROVE,ACCOUNT_STATUS_MANAGE,CUSTOMER_READ,CUSTOMER_UPDATE,KYC_VERIFY,PRODUCT_READ,PRODUCT_MANAGE,USER_MANAGE,ROLE_PERMISSION_MANAGE,BRANCH_MANAGE,EMPLOYEE_MANAGE,CONFIG_MANAGE,NOTIFICATION_MANAGE,AUDIT_VIEW', type: 'string' },
    { key: 'serviceName', value: callerServiceName(service.slug), type: 'string' },
    { key: 'idempotencyKey', value: '', type: 'string' },
    { key: 'correlationId', value: '', type: 'string' },
    ...Object.entries(variableExamples)
      .filter(([key]) => !['sessionId', 'branchCode', 'correlationId'].includes(key))
      .map(([key, value]) => ({ key, value, type: 'string' }))
  ];
}

function callerServiceName(slug) {
  const callers = {
    'account-service': 'transaction-service',
    'audit-service': 'account-service',
    'ledger-service': 'transaction-service',
    'notification-service': 'account-service',
    'statement-reporting-service': 'account-service',
    'transaction-service': 'account-service'
  };
  return callers[slug] || 'postman';
}

function writeJson(file, value) {
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`, 'utf8');
}

fs.mkdirSync(outputDir, { recursive: true });
const coverage = [];
for (const service of services) {
  const absoluteRoot = path.join(workspace, service.root);
  const controllers = parseControllers(absoluteRoot);
  const typeIndex = buildTypeIndex(absoluteRoot);
  const collection = collectionFor(service, controllers, typeIndex);
  const fileName = `${service.slug}.postman_collection.json`;
  writeJson(path.join(outputDir, fileName), collection);
  coverage.push({
    service: service.slug,
    port: service.port,
    collection: `collections/${fileName}`,
    controllerCount: controllers.length,
    requestCount: controllers.reduce((count, controller) => count + controller.methods.length, 0),
    controllers: controllers.map(controller => ({
      name: controller.name,
      source: controller.source,
      requests: controller.methods.map(method => `${method.httpMethod} ${method.path}`)
    }))
  });
}
writeJson(path.join(__dirname, 'coverage-report.json'), coverage);

const controllerCount = coverage.reduce((count, entry) => count + entry.controllerCount, 0);
const requestCount = coverage.reduce((count, entry) => count + entry.requestCount, 0);
console.log(`Generated ${services.length} collections covering ${controllerCount} controllers and ${requestCount} controller requests.`);
