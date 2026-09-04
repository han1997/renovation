// Converts js/data/knowledge.js + js/data/prices.js IIFE results to JSON assets.
// Run: node android/scripts/build-assets.js
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const root = path.resolve(__dirname, '..', '..');
const dataDir = path.join(root, 'js', 'data');

function runIife(file, exportName) {
    const src = fs.readFileSync(file, 'utf-8');
    const sandbox = {
        window: {},
        console,
    };
    vm.createContext(sandbox);
    vm.runInContext(src, sandbox, { filename: file });
    const exported = sandbox.window[exportName];
    if (!exported) {
        throw new Error(`IIFE in ${file} did not export ${exportName}`);
    }
    return exported;
}

const knowledge = runIife(path.join(dataDir, 'knowledge.js'), 'DATA');
const prices = runIife(path.join(dataDir, 'prices.js'), 'PRICES');

const knowledgeOut = {
    version: 1,
    totalDurationNote: knowledge.totalDurationNote,
    stages: knowledge.stages,
    checklists: knowledge.checklists,
    tipTopics: knowledge.tipTopics,
    tips: knowledge.tips,
    acceptIntro: knowledge.acceptIntro,
    styles: knowledge.styles,
    styleQuiz: knowledge.styleQuiz,
    materialTimeline: knowledge.materialTimeline,
    modes: knowledge.modes,
    whoBuilds: knowledge.whoBuilds,
    glossary: knowledge.glossary,
    spaceNeeds: knowledge.spaceNeeds,
};

const pricesOut = {
    version: 1,
    reserveRatio: 0.08,
    tiers: prices.tiers,
    grades: prices.grades,
    rates: prices.rates,
    reference: prices.reference,
};

const assetsDir = path.join(__dirname, '..', 'app', 'src', 'main', 'assets');
fs.mkdirSync(assetsDir, { recursive: true });

fs.writeFileSync(
    path.join(assetsDir, 'knowledge.json'),
    JSON.stringify(knowledgeOut, null, 2),
    'utf-8',
);
fs.writeFileSync(
    path.join(assetsDir, 'prices.json'),
    JSON.stringify(pricesOut, null, 2),
    'utf-8',
);

console.log('Built knowledge.json', JSON.stringify(knowledgeOut).length, 'bytes');
console.log('Built prices.json', JSON.stringify(pricesOut).length, 'bytes');