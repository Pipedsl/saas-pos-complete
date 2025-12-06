import { Component } from '@angular/core';
import { PrimeImportsModule } from '../../../prime-imports';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';
import { Password } from 'primeng/password';


@Component({
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule, PrimeImportsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  loginForm: FormGroup;
  loading = false;
  errorMsg = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  onSubmit() {
    if (this.loginForm.invalid) return;

    this.loading = true;
    this.errorMsg = '';

    const credentials = this.loginForm.value;

    this.authService.login(credentials).subscribe({
      next: (res) => {
        console.log('LOGIN EXITOSO:', res);
        this.loading = false;
        this.router.navigate(['/dashboard']);
        alert(`Bienvenido ${res.role}! token guardado.`);
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        this.errorMsg = 'Error de autenticación. Por favor, verifica tus credenciales.';
      }
    });

  }
}
