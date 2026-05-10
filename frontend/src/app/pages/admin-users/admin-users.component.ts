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
    this.ban(user, 'TEMPORARY');
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

    const hours = mode === 'TEMPORARY' ? 72 : undefined;

    this.adminService.banUser(adminId, user.id, mode, hours).subscribe({
      next: () => this.searchUsers(),
      error: () => this.error = 'No se pudo aplicar el baneo.'
    });
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
