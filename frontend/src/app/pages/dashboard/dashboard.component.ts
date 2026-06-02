import {
  AfterViewInit,
  Component,
  ElementRef,
  HostListener,
  Inject,
  OnDestroy,
  OnInit,
  ViewChild,
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
import { Observable, Subscription, catchError, interval, of, startWith, switchMap } from 'rxjs';
import { ThemeService } from '../../services/theme/theme.service';
import { svgIcons } from '../../icons/svg-icons';
import { AuthService } from '../../services/auth/auth.service';
import { AppNotification, NotificationService } from '../../services/notification/notification.service';
import { UserProfile, UserService } from '../../services/user/user.service';

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
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('notificationWrapper') notificationWrapper?: ElementRef<HTMLElement>;
  user$: Observable<any | null>;
  SidebarSectionEnum = SidebarSection;
  currentSection: SidebarSection = SidebarSection.ROOT;
  isMobileSidebarOpen = false;
  rootMenu: any[] = [];
  sanitizedIcons: { [key: string]: SafeHtml } = {};
  fadeIn = false;
  isDarkMode$: Observable<boolean>;
  notifications: AppNotification[] = [];
  isNotificationPanelOpen = false;
  isLoadingNotifications = false;
  unreadNotificationsCount = 0;
  private userSubscription?: Subscription;
  private banStatusPollingSubscription?: Subscription;
  private hasForcedLogoutForBan = false;

  constructor(
    private router: Router,
    private themeService: ThemeService,
    private sanitizer: DomSanitizer,
    private authService: AuthService,
    private notificationService: NotificationService,
    private userService: UserService,
    @Inject(DOCUMENT) private doc: Document
  ) {
    this.isDarkMode$ = this.themeService.isDarkMode$
    this.user$ = this.authService.user$;
  }

  ngOnInit(): void {
    this.userSubscription = this.authService.user$.subscribe(user => {
      this.updateMenu(user);
      if (user?.id) {
        this.hasForcedLogoutForBan = false;
        this.startBanStatusPolling(user.id);
        this.loadNotifications(user.id);
      } else {
        this.stopBanStatusPolling();
        this.notifications = [];
        this.unreadNotificationsCount = 0;
        this.isNotificationPanelOpen = false;
      }
    });

    for (const key of Object.keys(svgIcons) as Array<keyof typeof svgIcons>) {
      this.sanitizedIcons[key] = this.sanitizer.bypassSecurityTrustHtml(svgIcons[key]);
    }
  }

  updateMenu(user: any | null): void {
    if (user) {
      this.rootMenu = [
        { label: 'Inicio', route: '/home', icon: 'ICON_INICIO' },
        { label: 'Mascotas', route: '/pets', icon: 'ICON_PETS' },
        { label: 'Publicaciones', route: '/posts', icon: 'ICON_POSTS' },
        { label: 'Mi Perfil', route: '/profile', icon: 'ICON_USER' }
      ];

      if (user.role === 'ADMIN') {
        this.rootMenu = [
          ...this.rootMenu,
          { label: 'Admin Usuarios', route: '/admin/users', icon: 'ICON_ADMIN_USERS' },
          { label: 'Admin Contenido', route: '/admin/content', icon: 'ICON_ADMIN_CONTENT' },
          { label: 'Admin Notificar', route: '/admin/notifications', icon: 'ICON_ADMIN_NOTIFY' }
        ];
      }
    } else {
      this.rootMenu = [
        { label: 'Inicio', route: '/home', icon: 'ICON_INICIO' }
      ];
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

  ngOnDestroy(): void {
    this.userSubscription?.unsubscribe();
    this.stopBanStatusPolling();
  }

  logOut() {
    this.authService.logout();
    this.router.navigate(['/auth']);
  }

  toggleNotificationsPanel(): void {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return;
    }

    this.isNotificationPanelOpen = !this.isNotificationPanelOpen;
    if (this.isNotificationPanelOpen) {
      this.loadNotifications(userId);
    }
  }

  markNotificationAsRead(notification: AppNotification): void {
    if (notification.isRead) {
      return;
    }

    const userId = this.getCurrentUserId();
    if (!userId) {
      return;
    }

    this.notificationService.markAsRead(notification.id, userId).subscribe({
      next: () => {
        notification.isRead = true;
        this.reorderNotifications();
        this.refreshUnreadCounter();
      }
    });
  }

  markAllNotificationsAsRead(): void {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return;
    }

    this.notificationService.markAllAsRead(userId).subscribe({
      next: () => {
        this.notifications = this.notifications.map(notification => ({
          ...notification,
          isRead: true,
          readAt: notification.readAt || new Date().toISOString()
        }));
        this.reorderNotifications();
        this.refreshUnreadCounter();
      }
    });
  }

  toggleTheme() {
    this.themeService.toggleTheme();
  }

  toggleMobileSidebar() {
    this.isMobileSidebarOpen = !this.isMobileSidebarOpen;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.isNotificationPanelOpen) {
      return;
    }

    const wrapper = this.notificationWrapper?.nativeElement;
    const target = event.target as Node | null;

    if (!wrapper || !target) {
      return;
    }

    if (!wrapper.contains(target)) {
      this.isNotificationPanelOpen = false;
    }
  }

  private loadNotifications(userId: number): void {
    this.isLoadingNotifications = true;

    this.notificationService.getNotifications(userId).subscribe({
      next: notifications => {
        this.notifications = notifications;
        this.reorderNotifications();
        this.refreshUnreadCounter();
        this.isLoadingNotifications = false;
      },
      error: () => {
        this.isLoadingNotifications = false;
      }
    });
  }

  private reorderNotifications(): void {
    this.notifications = [...this.notifications].sort((a, b) => {
      if (a.isRead !== b.isRead) {
        return a.isRead ? 1 : -1;
      }
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });
  }

  private refreshUnreadCounter(): void {
    this.unreadNotificationsCount = this.notifications.filter(notification => !notification.isRead).length;
  }

  private getCurrentUserId(): number | null {
    const userRaw = localStorage.getItem('user');
    if (!userRaw) {
      return null;
    }

    try {
      const user = JSON.parse(userRaw) as { id?: number };
      return user.id ?? null;
    } catch {
      return null;
    }
  }

  private startBanStatusPolling(userId: number): void {
    this.stopBanStatusPolling();

    this.banStatusPollingSubscription = interval(30000)
      .pipe(
        startWith(0),
        switchMap(() => this.userService.getUserById(userId).pipe(catchError(() => of(null))))
      )
      .subscribe(userProfile => {
        if (!userProfile || this.hasForcedLogoutForBan) {
          return;
        }

        if (this.isUserCurrentlyBanned(userProfile)) {
          this.forceLogoutDueToBan(userProfile);
        }
      });
  }

  private stopBanStatusPolling(): void {
    this.banStatusPollingSubscription?.unsubscribe();
    this.banStatusPollingSubscription = undefined;
  }

  private isUserCurrentlyBanned(user: UserProfile): boolean {
    if (user.status === 'BANNED_PERMANENT') {
      return true;
    }

    if (user.status !== 'BANNED_TEMPORARY') {
      return false;
    }

    if (!user.bannedUntil) {
      return true;
    }

    return new Date(user.bannedUntil).getTime() > Date.now();
  }

  private forceLogoutDueToBan(user: UserProfile): void {
    this.hasForcedLogoutForBan = true;

    let message = 'Tu cuenta ha sido baneada. Se ha cerrado tu sesion.';
    if (user.status === 'BANNED_TEMPORARY' && user.bannedUntil) {
      message = `Tu cuenta esta baneada temporalmente hasta ${new Date(user.bannedUntil).toLocaleString()}. Se ha cerrado tu sesion.`;
    }

    if (user.status === 'BANNED_PERMANENT') {
      message = 'Tu cuenta ha sido baneada permanentemente. Se ha cerrado tu sesion.';
    }

    sessionStorage.setItem('banModalMessage', message);
    this.authService.logout();
    this.router.navigate(['/auth']);
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
