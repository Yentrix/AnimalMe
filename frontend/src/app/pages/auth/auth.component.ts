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
    this.isExiting = true;

    setTimeout(() => {
      this.isLogin = !this.isLogin;

      setTimeout(() => {
        this.isExiting = false;
      }, 50);
    }, 20);
  }

}
