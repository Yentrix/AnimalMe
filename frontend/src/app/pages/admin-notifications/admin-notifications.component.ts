import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminService, AdminUser } from '../../services/admin/admin.service';

@Component({
  selector: 'app-admin-notifications',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-notifications.component.html',
  styleUrl: './admin-notifications.component.css'
})
export class AdminNotificationsComponent {
  title = '';
  message = '';
  sendToAll = true;
  userQuery = '';
  userResults: AdminUser[] = [];
  selectedUsers: AdminUser[] = [];
  stateMessage = '';
  error = '';

  constructor(private adminService: AdminService) { }

  searchUsers(): void {
    if (this.sendToAll) {
      return;
    }

    const adminId = this.getCurrentUserId();
    if (!adminId || this.userQuery.trim().length < 2) {
      this.userResults = [];
      return;
    }

    this.adminService.searchUsers(adminId, this.userQuery).subscribe({
      next: users => this.userResults = users.filter(u => u.role !== 'ADMIN'),
      error: () => this.error = 'No se pudieron buscar usuarios.'
    });
  }

  toggleUser(user: AdminUser): void {
    const exists = this.selectedUsers.some(item => item.id === user.id);
    if (exists) {
      this.selectedUsers = this.selectedUsers.filter(item => item.id !== user.id);
      return;
    }

    this.selectedUsers = [...this.selectedUsers, user];
  }

  sendNotification(): void {
    const adminId = this.getCurrentUserId();
    if (!adminId) {
      this.error = 'Debes iniciar sesion como administrador.';
      return;
    }

    this.error = '';
    this.stateMessage = '';

    const payload = {
      title: this.title.trim(),
      message: this.message.trim(),
      sendToAll: this.sendToAll,
      userIds: this.selectedUsers.map(user => user.id)
    };

    this.adminService.sendNotification(adminId, payload).subscribe({
      next: response => {
        this.stateMessage = `Notificacion enviada a ${response.recipients} usuario(s).`;
      },
      error: () => {
        this.error = 'No se pudo enviar la notificacion.';
      }
    });
  }

  isSelected(userId: number): boolean {
    return this.selectedUsers.some(user => user.id === userId);
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
