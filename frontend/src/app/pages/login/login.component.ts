import { Component, ElementRef, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';
import { response } from 'express';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CommonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  loginForm: FormGroup;
  registerForm: FormGroup;

  isLogin = true;

  petImages = [
    'assets/pets-placeholder/pet1.jpg',
    'assets/pets-placeholder/pet2.jpg',
    'assets/pets-placeholder/pet3.jpg',
    'assets/pets-placeholder/pet4.jpg',
    'assets/pets-placeholder/pet5.jpg',
    'assets/pets-placeholder/pet6.jpg',
    'assets/pets-placeholder/pet7.jpg',
    'assets/pets-placeholder/pet8.jpg',
    'assets/pets-placeholder/pet9.jpg',
    'assets/pets-placeholder/pet10.jpg',
    'assets/pets-placeholder/pet11.jpg',
    'assets/pets-placeholder/pet12.jpg',
    'assets/pets-placeholder/pet13.webp',
    'assets/pets-placeholder/pet14.png',
    'assets/pets-placeholder/pet15.jpg',
    'assets/pets-placeholder/pet16.jpg',
    'assets/pets-placeholder/pet17.jpg',
    'assets/pets-placeholder/pet18.jpg',
    'assets/pets-placeholder/pet19.jpg',
    'assets/pets-placeholder/pet20.jpg',
    'assets/pets-placeholder/pet21.jpg',
    'assets/pets-placeholder/pet22.jpg',
    'assets/pets-placeholder/pet23.jpg',
    'assets/pets-placeholder/pet24.jpg',
  ];

  constructor(
    private fb_login: FormBuilder,
    private fb_register: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb_login.group({
      email: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });

    this.registerForm = this.fb_register.group({
      nombre: ['', [Validators.required]],
      email: ['', [Validators.required]],
      password: ['', [Validators.required]],
    });
  }

  @ViewChild('wrapper') wrapper!: ElementRef;
  @ViewChild('sideBanner') sideBanner!: ElementRef;
  ngAfterViewInit(){
    this.updateHeight();

    setTimeout(() => {
      this.sideBanner.nativeElement.classList.add('show')
    }, 500);
  }

  onLogin() {
    if (this.loginForm.valid) {
      const credentials = this.loginForm.value;

      this.authService.login(credentials).subscribe({
        next: (response) => {
          console.log('Login cliente exitoso', response);
          
          this.authService.setUserId(response.id);

          this.router.navigate(['/dashboard']);
        },
        error: () => alert("Credenciales incorrectas")
      });
    }
  }

  onRegister() {
    if (this.registerForm.valid) {
      this.authService.register(this.registerForm.value).subscribe({
        next: (response) => {
          console.log('Registro exitoso', response)
        },
        error: (err) => alert("Error al registrarse")
      });
    }
  }

  // Cambio de formulario de login a register y viceversa
  toggleForm(){
    const nextForm = this.wrapper.nativeElement.querySelector(
      this.isLogin ? '.register-form' : '.login-form'
    );

    if (nextForm) {
      const nextHeight = nextForm.scrollHeight;

      // Aplicar la altua antes de animar
      this.wrapper.nativeElement.style.height = nextHeight + 'px';
    }

    // Lanzar la animacion horizontal
    this.isLogin = !this.isLogin
  }

  updateHeight() {
    const active = this.wrapper.nativeElement.querySelector(
      this.isLogin ? '.login-form' : '.register-form'
    );

    if (!active) return;

    const height = active.scrollHeight;
    this.wrapper.nativeElement.style.height = height + 'px';
  }

  get infiniteImages() {
    return [...this.petImages, ...this.petImages]
  }
}
