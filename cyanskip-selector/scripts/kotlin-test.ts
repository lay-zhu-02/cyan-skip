import { runGradle } from './gradle.ts';

await runGradle([
  ':cyanskip-selector:jvmTest',
  ':cyanskip-selector:jsNodeTest',
]);
