import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PetService {
  private apiUrl = `${environment.apiUrl}/pets`;

  constructor(private http: HttpClient) { }

  /**
   * Crea una mascota enviando JSON, Strings y un Archivo en una sola petición.
   * @param formData Contenedor con 'pet' (JSON), 'image' (File), 'speciesName' y 'breedName'.
   * @param ownerId ID del usuario logueado.
   */
  createPet(formData: FormData, ownerId: number): Observable<any> {
    // Los parámetros simples se pueden pasar en la URL para que @RequestParam los capture
    const params = new HttpParams().set('ownerId', ownerId.toString());

    return this.http.post<any>(this.apiUrl, formData, { params });
  }

  /**
   * Obtiene las mascotas de un dueño específico.
   */
  getPetsByOwner(ownerId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/owner/${ownerId}`);
  }

  /**
   * Actualiza una mascota existente (puedes expandir el backend luego para esto)
   */
  updatePet(id: number, formData: FormData): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, formData);
  }
}
