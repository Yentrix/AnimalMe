import {
  AfterViewInit,
  Component,
  Inject,
  OnInit,
} from '@angular/core';

import {
  RouterOutlet,
  RouterLink,
  RouterLinkActive,
  Router,
  NavigationEnd
} from '@angular/router';

import { NgClass, AsyncPipe, CommonModule, DOCUMENT } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Observable } from 'rxjs';
import { ThemeService } from '../../services/theme/theme.service';
import { svgIcons } from '../../icons/svg-icons';

export enum SidebarSection {
  ROOT = 0,
  // PET = 1,
}

interface SidebarItem {
  label: string;
  icon?: string;
  route?: string;
  section?: SidebarSection;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    RouterOutlet,
    AsyncPipe,
    RouterLink,
    RouterLinkActive,
    CommonModule,
    NgClass
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, AfterViewInit {
  SidebarSectionEnum = SidebarSection;
  currentSection: SidebarSection = SidebarSection.ROOT;
  isMobileSidebarOpen = false;
  rootMenu: any[] = [];
  sanitizedIcons: { [key: string]: SafeHtml } = {};
  fadeIn = false;
  isDarkMode$: Observable<boolean>;

  constructor(
    private router: Router,
    private themeService: ThemeService,
    private sanitizer: DomSanitizer,
    @Inject(DOCUMENT) private doc: Document
  ) {
    this.isDarkMode$ = this.themeService.isDarkMode$
  }

  ngOnInit(): void {
    this.rootMenu = [
      { label: 'Inicio', route: '/home', icon: 'ICON_INICIO' },
      { label: 'Mascotas', route: '/pets', icon: 'ICON_PETS' }, // Añadido según tus rutas [cite: 116]
      { label: 'Publicaciones', route: '/posts', icon: 'ICON_POSTS' },
      { label: 'Mi Perfil', route: '/profile', icon: 'ICON_USER' }
    ];

    for (const key of Object.keys(svgIcons) as Array<keyof typeof svgIcons>) {
      this.sanitizedIcons[key] = this.sanitizer.bypassSecurityTrustHtml(svgIcons[key]);
    }

    this.backToRoot();
  }

  onActivate() {
    this.fadeIn = false;
    setTimeout(() => this.fadeIn = true, 10);
  }

  async backToRoot() {
    await this.animateMenuChange();

    this.currentSection = SidebarSection.ROOT;

    setTimeout(() => {
      const newItems = document.querySelectorAll('.menu-item');
      newItems.forEach((el: any, i: number) => {
        el.classList.remove('fade-out');
        el.style.setProperty('--delay', i.toString());
        el.classList.add('fade-in-start');
      });
    }, 10);
  }

  ngAfterViewInit(): void {
    this.router.events.subscribe(ev => {
      if (ev instanceof NavigationEnd)
        this.isMobileSidebarOpen = false;
    });
  }

  logOut() {
    // this.authService.logout();
  }

  toggleTheme() {
    this.themeService.toggleTheme();
  }

  toggleMobileSidebar() {
    this.isMobileSidebarOpen = !this.isMobileSidebarOpen;
  }

  triggerSidebarAnimation() {
    setTimeout(() => {
      const items = document.querySelectorAll('.animate-items > .menu-item');
      items.forEach(el => {
        el.classList.remove('animate-start');
        void (el as HTMLElement).offsetWidth;
        el.classList.add('animate-start');
      });
    });
  }

  async openSection(section: SidebarSection) {
    await this.animateMenuChange();

    this.currentSection = section;

    setTimeout(() => {
      const newItems = document.querySelectorAll('.menu-item');
      newItems.forEach((el: any, i: number) => {
        el.classList.remove('fade-out');
        el.style.setProperty('--delay', i.toString());
        el.classList.add('fade-in-start');
      });
    }, 10);
  }

  animateMenuChange() {
    const items = Array.from(document.querySelectorAll('.menu-item'));

    items.forEach((item: any, idx: number) => {
      item.classList.remove('fade-in-start');
      item.style.setProperty('--delay', idx.toString());
      item.classList.add('fade-out');
    });

    return new Promise(resolve => setTimeout(resolve, 250));
  }
}
