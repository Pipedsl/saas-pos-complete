import { Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { HttpClient } from "@angular/common/http";
import { Supplier } from "../models/supplier.model";


@Injectable({
    providedIn: 'root'
})

export class SuppliersService {

    private apiUrl = `${environment.apiUrl}/api/suppliers`;

    constructor(private http: HttpClient) { }

    getSuppliers() {
        return this.http.get<Supplier[]>(this.apiUrl);
    }

    createSupplier(supplier: Supplier) {
        return this.http.post<Supplier>(this.apiUrl, supplier);
    }
}