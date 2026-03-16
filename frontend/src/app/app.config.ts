import { ApplicationConfig, LOCALE_ID, importProvidersFrom } from '@angular/core';
import localeEs from '@angular/common/locales/es'
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes), 
    provideClientHydration(),
    provideHttpClient(),
    { provide: LOCALE_ID, useValue: 'es-ES'}
  ]
};
