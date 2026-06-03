import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  @Output() switchToLogin = new EventEmitter<void>();

  userData = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    country: '',
    city: ''
  };

  errorMessage = '';

  get passwordStrengthLabel(): string {
    const score = this.getPasswordScore(this.userData.password);
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
    return (this.getPasswordScore(this.userData.password) / 4) * 100;
  }

  get canSubmitRegister(): boolean {
    return this.userData.password.trim().length >= 8;
  }

  constructor(private authService: AuthService) { }

  onRegister() {
    this.errorMessage = '';
    if (!this.canSubmitRegister) {
      this.errorMessage = 'La contraseña debe tener al menos 8 caracteres.';
      return;
    }

    this.authService.register(this.userData).subscribe({
      next: () => this.switchToLogin.emit(),
      error: (err) => {
        console.error('Error en registro', err);
        this.errorMessage = 'No se pudo crear la cuenta. El email ya existe o los datos son inválidos.';
      }
    });
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
