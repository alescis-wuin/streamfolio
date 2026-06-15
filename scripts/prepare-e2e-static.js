const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const source = path.join(root, 'backend', 'src', 'main', 'resources', 'static');
const target = path.join(root, 'backend', 'target', 'classes', 'static');

if (!fs.existsSync(source)) {
  throw new Error(`Static resources directory not found: ${source}`);
}

fs.rmSync(target, { recursive: true, force: true });
fs.mkdirSync(path.dirname(target), { recursive: true });
fs.cpSync(source, target, { recursive: true });
console.log(`Copied static resources to ${path.relative(root, target)}`);
