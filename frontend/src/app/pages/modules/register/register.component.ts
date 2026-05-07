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

  constructor(private authService: AuthService) { }

  onRegister() {
    this.errorMessage = '';
    this.authService.register(this.userData).subscribe({
      next: () => this.switchToLogin.emit(),
      error: (err) => {
        console.error('Error en registro', err);
        this.errorMessage = 'No se pudo crear la cuenta. El email ya existe o los datos son inválidos.';
      }
    });
  }
}
