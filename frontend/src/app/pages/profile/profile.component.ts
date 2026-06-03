import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth/auth.service';
import { UserProfile, UserService } from '../../services/user/user.service';

interface AuthUser {
  id?: number;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  profileData = {
    firstName: '',
    email: '',
    contactEmail: '',
    contactPhone: ''
  };

  passwordData = {
    currentPassword: '',
    newPassword: '',
    confirmNewPassword: ''
  };

  isLoading = false;
  isSavingProfile = false;
  isSavingPassword = false;

  profileError = '';
  profileSuccess = '';
  passwordError = '';
  passwordSuccess = '';

  constructor(
    private userService: UserService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.loadProfile();
  }

  get newPasswordTooShort(): boolean {
    return this.passwordData.newPassword.length > 0 && this.passwordData.newPassword.length < 8;
  }

  get passwordsMismatch(): boolean {
    return this.passwordData.confirmNewPassword.length > 0
      && this.passwordData.newPassword !== this.passwordData.confirmNewPassword;
  }

  get canSubmitPassword(): boolean {
    return this.passwordData.currentPassword.trim().length > 0
      && this.passwordData.newPassword.trim().length >= 8
      && !this.passwordsMismatch;
  }

  get passwordStrengthLabel(): string {
    const score = this.getPasswordScore(this.passwordData.newPassword);
    if (score <= 1) {
      return 'Muy debil';
    }
    if (score <= 2) {
      return 'Debil';
    }
    if (score <= 3) {
      return 'Media';
    }
    return 'Fuerte';
  }

  get passwordStrengthPercent(): number {
    return (this.getPasswordScore(this.passwordData.newPassword) / 4) * 100;
  }

  saveProfile(): void {
    const userId = this.getCurrentUserId();
    if (!userId) {
      this.profileError = 'No se encontro la sesion del usuario.';
      return;
    }

    this.profileError = '';
    this.profileSuccess = '';
    this.isSavingProfile = true;

    this.userService.updateProfile(userId, {
      firstName: this.profileData.firstName.trim(),
      email: this.profileData.email.trim(),
      contactEmail: this.profileData.contactEmail.trim(),
      contactPhone: this.profileData.contactPhone.trim()
    }).subscribe({
      next: (user) => {
        this.profileSuccess = 'Perfil actualizado correctamente.';
        this.isSavingProfile = false;
        this.syncLocalUser(user);
      },
      error: (err) => {
        this.profileError = err?.error?.message || 'No se pudo actualizar el perfil.';
        this.isSavingProfile = false;
      }
    });
  }

  changePassword(): void {
    if (!this.canSubmitPassword) {
      this.passwordError = 'Revisa los campos de la contraseña.';
      return;
    }

    const userId = this.getCurrentUserId();
    if (!userId) {
      this.passwordError = 'No se encontro la sesion del usuario.';
      return;
    }

    this.passwordError = '';
    this.passwordSuccess = '';
    this.isSavingPassword = true;

    this.userService.updatePassword(userId, {
      currentPassword: this.passwordData.currentPassword,
      newPassword: this.passwordData.newPassword,
      confirmNewPassword: this.passwordData.confirmNewPassword
    }).subscribe({
      next: () => {
        this.passwordSuccess = 'Contraseña actualizada correctamente.';
        this.isSavingPassword = false;
        this.passwordData = {
          currentPassword: '',
          newPassword: '',
          confirmNewPassword: ''
        };
      },
      error: (err) => {
        this.passwordError = err?.error?.message || 'No se pudo actualizar la contraseña.';
        this.isSavingPassword = false;
      }
    });
  }

  private loadProfile(): void {
    const userId = this.getCurrentUserId();
    if (!userId) {
      this.profileError = 'No se encontro la sesion del usuario.';
      return;
    }

    this.isLoading = true;
    this.profileError = '';

    this.userService.getUserById(userId).subscribe({
      next: (user) => {
        this.profileData = {
          firstName: user.firstName || '',
          email: user.email || '',
          contactEmail: user.contactEmail || user.email || '',
          contactPhone: user.contactPhone || ''
        };
        this.isLoading = false;
      },
      error: () => {
        this.profileError = 'No se pudo cargar el perfil.';
        this.isLoading = false;
      }
    });
  }

  private syncLocalUser(user: UserProfile): void {
    const raw = localStorage.getItem('user');
    if (!raw) {
      return;
    }

    const current = JSON.parse(raw);
    const updated = {
      ...current,
      firstName: user.firstName,
      email: user.email,
      contactEmail: user.contactEmail,
      contactPhone: user.contactPhone
    };
    this.authService.syncUser(updated);
  }

  private getCurrentUserId(): number | null {
    const userJson = localStorage.getItem('user');
    if (!userJson) {
      return null;
    }
    const user = JSON.parse(userJson) as AuthUser;
    return user.id ?? null;
  }

  private getPasswordScore(password: string): number {
    if (!password) {
      return 0;
    }

    let score = 0;
    if (password.length >= 8) {
      score += 1;
    }
    if (/[A-Z]/.test(password) && /[a-z]/.test(password)) {
      score += 1;
    }
    if (/\d/.test(password)) {
      score += 1;
    }
    if (/[^A-Za-z0-9]/.test(password)) {
      score += 1;
    }
    return score;
  }

}
