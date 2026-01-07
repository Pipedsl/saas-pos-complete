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
import { CheckboxModule } from "primeng/checkbox";
import { RadioButtonModule } from 'primeng/radiobutton';
import { TabsModule } from 'primeng/tabs';
import { ColorPickerModule } from 'primeng/colorpicker';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { FileUploadModule } from 'primeng/fileupload';
import { MultiSelectModule } from 'primeng/multiselect';
import { TooltipModule } from 'primeng/tooltip';
import { ChartModule } from 'primeng/chart';
import { SelectModule } from "primeng/select";






@NgModule({

    imports: [ButtonModule, CardModule, InputTextModule, PasswordModule, TableModule, TagModule, InputNumberModule, TextareaModule, DialogModule, ToastModule, AutoCompleteModule, CheckboxModule, RadioButtonModule, TabsModule, ColorPickerModule, InputGroupModule, InputGroupAddonModule, FileUploadModule, MultiSelectModule, InputNumberModule, TooltipModule, ChartModule, SelectModule
    ],
    exports: [ButtonModule, CardModule, InputTextModule, PasswordModule, TableModule, TagModule, InputNumberModule, TextareaModule, DialogModule, ToastModule, AutoCompleteModule, CheckboxModule, RadioButtonModule, TabsModule, ColorPickerModule, InputGroupModule, InputGroupAddonModule, FileUploadModule, MultiSelectModule, InputNumberModule, TooltipModule, ChartModule, SelectModule],
    providers: [MessageService]

})
export class PrimeImportsModule { }
