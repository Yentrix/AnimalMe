import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PublicationAuthor {
  id: number;
  firstName?: string;
  lastName?: string;
  email?: string;
  contactEmail?: string;
  contactPhone?: string;
}

export interface PublicationPet {
  id: number;
  name: string;
  age?: number;
  sex?: string;
  sizeCm?: number;
  description?: string;
  species?: { id?: number; name?: string };
  breed?: { id?: number; name?: string };
  images?: PublicationImage[];
}

export interface PublicationImage {
  id: number;
  url: string;
}

export interface PublicationSummary {
  id: number;
  title: string;
  description: string;
  createdAt: string;
  status: 'OPEN' | 'CLOSED' | 'ARCHIVED';
  adoptionStatus: 'AVAILABLE' | 'URGENT' | 'ADOPTED';
  author?: PublicationAuthor;
  pets?: PublicationPet[];
  images?: PublicationImage[];
  pendingRequestsCount?: number;
}

export interface PublicationUpdatePayload {
  title: string | null;
  description: string;
  adoptionStatus: 'AVAILABLE' | 'URGENT' | 'ADOPTED';
  petIds: number[];
  removedImageIds: number[];
}

export interface AdoptionRequestSummary {
  id: number;
  message: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'ARCHIVED';
  createdAt: string;
  applicant?: PublicationAuthor;
}

@Injectable({
  providedIn: 'root'
})
export class PublicationService {
  private apiUrl = `${environment.apiUrl}/publications`;

  constructor(private http: HttpClient) { }

  createPublication(formData: FormData, authorId: number): Observable<PublicationSummary> {
    const params = new HttpParams().set('authorId', authorId.toString());
    return this.http.post<PublicationSummary>(this.apiUrl, formData, { params });
  }

  getActivePublications(): Observable<PublicationSummary[]> {
    return this.http.get<PublicationSummary[]>(`${this.apiUrl}/active`);
  }

  getPublicationsByAuthor(authorId: number): Observable<PublicationSummary[]> {
    return this.http.get<PublicationSummary[]>(`${this.apiUrl}/author/${authorId}`);
  }

  updatePublication(publicationId: number, authorId: number, payload: PublicationUpdatePayload, files: File[]): Observable<PublicationSummary> {
    const params = new HttpParams().set('authorId', authorId.toString());
    const formData = new FormData();
    formData.append('publication', JSON.stringify(payload));
    files.forEach(file => formData.append('images', file));
    return this.http.put<PublicationSummary>(`${this.apiUrl}/${publicationId}`, formData, { params });
  }

  createAdoptionRequest(publicationId: number, applicantId: number, message: string): Observable<AdoptionRequestSummary> {
    const params = new HttpParams().set('applicantId', applicantId.toString());
    return this.http.post<AdoptionRequestSummary>(`${this.apiUrl}/${publicationId}/adoption-requests`, { message }, { params });
  }

  getAdoptionRequests(publicationId: number, authorId: number): Observable<AdoptionRequestSummary[]> {
    const params = new HttpParams().set('authorId', authorId.toString());
    return this.http.get<AdoptionRequestSummary[]>(`${this.apiUrl}/${publicationId}/adoption-requests`, { params });
  }

  updateAdoptionRequestStatus(
    publicationId: number,
    requestId: number,
    authorId: number,
    status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'ARCHIVED'
  ): Observable<AdoptionRequestSummary> {
    const params = new HttpParams()
      .set('authorId', authorId.toString())
      .set('status', status);
    return this.http.put<AdoptionRequestSummary>(`${this.apiUrl}/${publicationId}/adoption-requests/${requestId}`, {}, { params });
  }

  deleteAdoptionRequest(publicationId: number, requestId: number, authorId: number): Observable<void> {
    const params = new HttpParams().set('authorId', authorId.toString());
    return this.http.delete<void>(`${this.apiUrl}/${publicationId}/adoption-requests/${requestId}`, { params });
  }

  deletePublication(publicationId: number, authorId: number): Observable<void> {
    const params = new HttpParams().set('authorId', authorId.toString());
    return this.http.delete<void>(`${this.apiUrl}/${publicationId}`, { params });
  }
}
