import { Component } from '@angular/core';
import { LoginComponent } from "../modules/login/login.component";
import { RegisterComponent } from "../modules/register/register.component";

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [LoginComponent, RegisterComponent],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.css'
})
export class AuthComponent {

}
