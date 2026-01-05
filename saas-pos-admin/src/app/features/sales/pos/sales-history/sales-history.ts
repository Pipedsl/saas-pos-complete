import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PrimeImportsModule } from '../../../../prime-imports';
import { SalesService, Sale } from '../../../../core/services/sales.service';

@Component({
    selector: 'app-sales-history',
    standalone: true,
    imports: [CommonModule, RouterModule, PrimeImportsModule],
    templateUrl: './sales-history.html',
})
export class SalesHistoryComponent implements OnInit {
    sales: Sale[] = [];
    loading = true;

    constructor(
        private salesService: SalesService,
        private cd: ChangeDetectorRef
    ) { }

    ngOnInit() {
        this.salesService.getSales().subscribe({
            next: (data) => {
                this.sales = data;
                this.loading = false;
                this.cd.detectChanges();
            },
            error: (err) => {
                console.error(err);
                this.loading = false;
                this.cd.detectChanges();
            }
        });
    }
}