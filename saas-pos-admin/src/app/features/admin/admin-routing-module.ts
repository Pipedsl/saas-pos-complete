import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { WebOrderDetailComponent } from './web-order-detail/web-order-detail';

const routes: Routes = [
  { path: 'web-orders/edit/:orderNumber', component: WebOrderDetailComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
