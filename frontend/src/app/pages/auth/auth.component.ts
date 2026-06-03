import { Component } from '@angular/core';
import { LoginComponent } from "../modules/login/login.component";
import { RegisterComponent } from "../modules/register/register.component";
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
  animationState: 'idle' | 'exiting' | 'entering' = 'idle';

  toggleAuth() {
    if (this.isExiting) {
      return;
    }

    this.animationState = 'exiting';
    this.isExiting = true;

    setTimeout(() => {
      this.isLogin = !this.isLogin;
      this.animationState = 'entering';

      setTimeout(() => {
        this.isExiting = false;
        this.animationState = 'idle';
      }, 260);
    }, 300);
  }
}
