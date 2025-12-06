import { NgModule } from "@angular/core";
import { ButtonModule } from "primeng/button";
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { InputNumberModule } from 'primeng/inputnumber';
import { TextareaModule } from 'primeng/textarea';
import { DialogModule } from 'primeng/dialog';
import { ToastModule } from 'primeng/toast'; // Para alertas
import { MessageService } from 'primeng/api'; // Para alertas
import { AutoCompleteModule } from "primeng/autocomplete";






@NgModule({

    imports: [ButtonModule, CardModule, InputTextModule, PasswordModule, TableModule, TagModule, InputNumberModule, TextareaModule, DialogModule, ToastModule, AutoCompleteModule
    ],
    exports: [ButtonModule, CardModule, InputTextModule, PasswordModule, TableModule, TagModule, InputNumberModule, TextareaModule, DialogModule, ToastModule, AutoCompleteModule],
    providers: [MessageService]

})
export class PrimeImportsModule { }
