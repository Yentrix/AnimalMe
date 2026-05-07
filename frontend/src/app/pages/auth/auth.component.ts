import { Component } from '@angular/core';
import { LoginComponent } from "../modules/login/login.component";
import { RegisterComponent } from "../modules/register/register.component";
import { trigger, transition, style, animate } from '@angular/animations';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, LoginComponent, RegisterComponent],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.css',
})
export class AuthComponent {
  isLogin = true;
  isExiting = false;

  toggleAuth() {
    // 1. Iniciamos la desaparición
    this.isExiting = true;

    // 2. Un retraso mínimo para que el navegador registre el cambio de clase
    // pero lo suficientemente rápido para que la tarjeta empiece a crecer ya.
    setTimeout(() => {
      this.isLogin = !this.isLogin;

      // 3. Volvemos a mostrar el contenido
      setTimeout(() => {
        this.isExiting = false;
      }, 50);
    }, 20);
  }

}
