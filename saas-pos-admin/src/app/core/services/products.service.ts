import { Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { HttpClient } from "@angular/common/http";
import { map, Observable } from "rxjs";
import { Product } from "../../core/models/product.model";

@Injectable({
    providedIn: 'root'
})
export class ProductsService {
    //Apunta a http://localhost:8081/api/products
    private apiUrl = `${environment.apiUrl}/api/products`;

    constructor(private http: HttpClient) { }

    getProducts(): Observable<Product[]> {
        return this.http.get<Product[]>(this.apiUrl);
    }

    createProduct(product: Partial<Product>): Observable<Product> {
        return this.http.post<Product>(this.apiUrl, product);
    }

    getProductBySku(sku: string): Observable<Product> {
        return this.http.get<Product>(`${this.apiUrl}/sku/${sku}`);
    }

    getProductById(id: string): Observable<Product> {
        return this.http.get<Product>(`${this.apiUrl}/${id}`);
    }

    updateProduct(id: string, product: Partial<Product>): Observable<Product> {
        return this.http.put<Product>(`${this.apiUrl}/${id}`, product);
    }

    deleteProduct(id: string, force: boolean = false): Observable<any> {
        // Pasamos el query param ?force=true si el usuario elige borrar todo
        return this.http.delete(`${this.apiUrl}/${id}?force=${force}`);
    }

    searchProducts(query: string): Observable<Product[]> {
        // CAMBIO: Ahora llamamos al backend que busca por SKU, Nombre y Categoría
        return this.http.get<Product[]>(`${this.apiUrl}/search?q=${query}`);
    }

    getLowStockProducts(): Observable<Product[]> {
        return this.http.get<Product[]>(`${this.apiUrl}/low-stock`);
    }

    activateProduct(id: string): Observable<any> {
        // Asegúrate de que 'this.apiUrl' apunte a '/api/products'
        return this.http.patch(`${this.apiUrl}/${id}/activate`, {});
    }
}