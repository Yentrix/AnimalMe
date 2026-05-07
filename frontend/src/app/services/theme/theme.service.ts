import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private isDarkMode = new BehaviorSubject<boolean>(this.getInitialTheme());
  public isDarkMode$ = this.isDarkMode.asObservable();

  constructor() {
    if (this.isBrowser()) {
      this.applyTheme(this.isDarkMode.value);
    }
  }

  private isBrowser(): boolean {
    return typeof window !== 'undefined' && typeof document !== 'undefined';
  }

  private getInitialTheme(): boolean {
    if (!this.isBrowser()) {
      return false;
    }

    // Primero intenta obtener del localStorage
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme !== null) {
      return savedTheme === 'dark';
    }

    // Si no hay entrada en localStorage, verifica preferencia del sistema
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return true;
    }

    return false;
  }

  toggleTheme(): void {
    const newTheme = !this.isDarkMode.value;
    this.isDarkMode.next(newTheme);
    if (this.isBrowser()) {
      this.applyTheme(newTheme);
      localStorage.setItem('theme', newTheme ? 'dark' : 'light');
    }
  }

  private applyTheme(isDark: boolean): void {
    if (!this.isBrowser()) {
      return;
    }

    const htmlElement = document.documentElement;
    if (isDark) {
      htmlElement.setAttribute('data-theme', 'dark');
    } else {
      htmlElement.removeAttribute('data-theme');
    }
  }

  isDarkModeActive(): boolean {
    return this.isDarkMode.value;
  }
}
