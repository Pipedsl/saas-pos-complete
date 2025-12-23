import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { StoreLayoutComponent } from './store-layout/store-layout';
import { CatalogComponent } from './pages/catalog/catalog';
import { CheckoutComponent } from './pages/checkout/checkout';

const routes: Routes = [
  {
    path: ':slug', // El parámetro mágico (ej: "tienda-demo")
    component: StoreLayoutComponent,
    children: [
      { path: '', component: CatalogComponent }, // Home de la tienda (Catálogo)
      { path: 'checkout', component: CheckoutComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class StoreRoutingModule { }
