import { ErrorHandler, Injectable, inject } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { TelemetryService } from '../telemetry.service'; // ajusta el path si lo guardas en otro sitio

@Injectable({ providedIn: 'root' })
export class GlobalErrorHandler implements ErrorHandler {
  private toastr = inject(ToastrService);
  private telemetry = inject(TelemetryService);

  handleError(error: any): void {
    this.telemetry.logToConsole?.('error', 'Unhandled error', { error });
    this.toastr.error('Ocurrió un error inesperado', 'Ups');
    console.error(error);
  }
}
