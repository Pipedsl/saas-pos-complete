import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { OnboardingService } from '../services/onboarding.service';
import { CommonModule } from '@angular/common';
import { PrimeImportsModule } from '../../../prime-imports';

@Component({
  selector: 'app-register',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PrimeImportsModule],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class RegisterComponent implements OnInit {

  registerForm: FormGroup;
  loading = false;
  token: string | null = null;
  errorMsg = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private onboardingService: OnboardingService
  ) {
    this.registerForm = this.fb.group({
      companyName: ['', Validators.required],
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');

    if (!this.token) {
      this.errorMsg = 'El enlace de registro es inválido o ha expirado.';
      this.registerForm.disable();
    }
  }

  onSubmit() {
    if (this.registerForm.invalid || !this.token) return;

    this.loading = true;
    this.errorMsg = '';

    const payload = {
      ...this.registerForm.value,
      token: this.token
    };

    this.onboardingService.register(payload).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        this.errorMsg = err.error || 'Error al registrar. El link puede haber expirado.';
      }
    });
  }
}