import { Component, OnInit } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-main-layout',
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css',
})
export class MainLayoutComponent implements OnInit {
  isMobileMenuOpen: boolean = false;
  currentUser: any = {};
  constructor(public authService: AuthService, private router: Router, public themeService: ThemeService) { }

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUser();

    this.authService.refreshUser().subscribe({
      next: () => {
        this.currentUser = this.authService.getCurrentUser();
      },
      error: () => {

      }
    })
  }

  toggleMobileMenu() {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  closeMobileMenu() {
    this.isMobileMenuOpen = false;
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
