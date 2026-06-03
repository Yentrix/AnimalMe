import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
  @Output() switchToRegister = new EventEmitter<void>();

  credentials = { email: '', password: '' };
  errorMessage = '';
  showBanModal = false;
  banModalMessage = '';

  constructor(private authService: AuthService, private router: Router) { }

  ngOnInit(): void {
    const banMessage = sessionStorage.getItem('banModalMessage');
    if (!banMessage) {
      return;
    }

    this.banModalMessage = banMessage;
    this.showBanModal = true;
    sessionStorage.removeItem('banModalMessage');
  }

  onLogin() {
    this.errorMessage = '';
    this.authService.login(this.credentials).subscribe({
      next: () => this.router.navigate(['/home']),
      error: (err) => {
        const errorMessage = this.extractErrorMessage(err).toLowerCase();
        if (errorMessage.includes('baneada')) {
          this.banModalMessage = this.extractErrorMessage(err);
          this.showBanModal = true;
          return;
        }

        this.errorMessage = 'Credenciales incorrectas';
      }
    });
  }

  closeBanModal(): void {
    this.showBanModal = false;
  }

  private extractErrorMessage(err: any): string {
    if (typeof err?.error === 'string') {
      return err.error;
    }

    if (typeof err?.error?.message === 'string') {
      return err.error.message;
    }

    if (typeof err?.message === 'string') {
      return err.message;
    }

    return 'Tu cuenta esta baneada temporalmente.';
  }
}
