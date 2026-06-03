import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminService, AdminUser } from '../../services/admin/admin.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.css'
})
export class AdminUsersComponent implements OnInit {
  users: AdminUser[] = [];
  query = '';
  loading = false;
  error = '';
  showBanModal = false;
  selectedUserForBan: AdminUser | null = null;
  banDuration = {
    days: 0,
    hours: 0,
    minutes: 0
  };

  constructor(private adminService: AdminService) { }

  ngOnInit(): void {
    this.searchUsers();
  }

  searchUsers(): void {
    const adminId = this.getCurrentUserId();
    if (!adminId) {
      this.error = 'Debes iniciar sesion como administrador.';
      return;
    }

    this.loading = true;
    this.error = '';

    this.adminService.searchUsers(adminId, this.query).subscribe({
      next: users => {
        this.users = users;
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar los usuarios.';
        this.loading = false;
      }
    });
  }

  banTemporary(user: AdminUser): void {
    this.selectedUserForBan = user;
    this.banDuration = { days: 0, hours: 24, minutes: 0 };
    this.showBanModal = true;
  }

  banPermanent(user: AdminUser): void {
    this.ban(user, 'PERMANENT');
  }

  unban(user: AdminUser): void {
    const adminId = this.getCurrentUserId();
    if (!adminId) {
      return;
    }

    this.adminService.unbanUser(adminId, user.id).subscribe({
      next: () => this.searchUsers(),
      error: () => this.error = 'No se pudo quitar el baneo.'
    });
  }

  private ban(user: AdminUser, mode: 'TEMPORARY' | 'PERMANENT'): void {
    const adminId = this.getCurrentUserId();
    if (!adminId) {
      return;
    }

    const duration = mode === 'TEMPORARY' ? this.banDuration : undefined;

    this.adminService.banUser(adminId, user.id, mode, duration).subscribe({
      next: () => this.searchUsers(),
      error: () => this.error = 'No se pudo aplicar el baneo.'
    });
  }

  confirmTemporaryBan(): void {
    if (!this.selectedUserForBan) {
      return;
    }

    const totalMinutes = this.banDuration.days * 24 * 60 + this.banDuration.hours * 60 + this.banDuration.minutes;
    if (totalMinutes <= 0) {
      this.error = 'Debes configurar una duracion mayor a 0 para el baneo temporal.';
      return;
    }

    this.ban(this.selectedUserForBan, 'TEMPORARY');
    this.closeBanModal();
  }

  closeBanModal(): void {
    this.showBanModal = false;
    this.selectedUserForBan = null;
  }

  formatStatus(user: AdminUser): string {
    if (user.status === 'BANNED_PERMANENT') {
      return 'Baneado permanente';
    }

    if (user.status === 'BANNED_TEMPORARY') {
      return user.bannedUntil ? `Baneado hasta ${new Date(user.bannedUntil).toLocaleString()}` : 'Baneado temporal';
    }

    return 'Activo';
  }

  private getCurrentUserId(): number | null {
    const raw = localStorage.getItem('user');
    if (!raw) {
      return null;
    }

    try {
      const user = JSON.parse(raw) as { id?: number };
      return user.id ?? null;
    } catch {
      return null;
    }
  }
}
