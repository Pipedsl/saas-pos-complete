import { Injectable } from "@angular/core";
import { environment } from "../../../../environments/environment";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { User } from "../../../core/models/user.model";


@Injectable({
    providedIn: 'root'
})
export class UsersService {
    private apiUrl = `${environment.apiUrl}/api/users`;

    constructor(
        private http: HttpClient
    ) { }

    //Listar equipo
    getUsers(): Observable<User[]> {
        return this.http.get<User[]>(this.apiUrl);
    }

    //Crear usuario
    createUser(user: any): Observable<User> {
        return this.http.post<User>(this.apiUrl, user);
    }
}