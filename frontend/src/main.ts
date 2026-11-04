import { bootstrapApplication } from '@angular/platform-browser';

import { App } from './app/app';
import { createApplicationConfig } from './app/app.config';
import { loadRuntimeConfig } from './app/core/config/runtime-config-loader';

/**
 * Loads environment configuration and starts Angular.
 */
async function startApplication(): Promise<void> {
  const runtimeConfig = await loadRuntimeConfig();

  await bootstrapApplication(App, createApplicationConfig(runtimeConfig));
}

void startApplication().catch((error: unknown) => {
  console.error('Bank System frontend failed to start.', error);
});
