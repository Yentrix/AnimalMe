import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AdoptionStatus } from '../../enums/adoption-status';
import {
  AdoptionRequestSummary,
  PublicationService,
  PublicationSummary
} from '../../services/publication/publication.service';

interface AuthUser {
  id?: number;
}

type PublicationModalView = 'menu' | 'edit' | 'requests';

@Component({
  selector: 'app-posts',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './posts.component.html',
  styleUrl: './posts.component.css'
})
export class PostsComponent implements OnInit {
  publicationEditForm!: FormGroup;
  publications: PublicationSummary[] = [];
  publicationImageIndex: Record<number, number> = {};
  selectedPublication: PublicationSummary | null = null;
  publicationModalView: PublicationModalView = 'menu';
  isPublicationModalOpen = false;
  adoptionRequests: AdoptionRequestSummary[] = [];
  isLoadingRequests = false;
  publicationActionMessage = '';
  publicationActionError = '';
  isSavingPublication = false;

  adoptionStatuses = Object.values(AdoptionStatus);
  readonly statusLabels: Record<AdoptionStatus, string> = {
    [AdoptionStatus.AVAILABLE]: 'Se puede adoptar',
    [AdoptionStatus.URGENT]: 'Urgente de adopcion',
    [AdoptionStatus.ADOPTED]: 'Adoptado'
  };

  readonly requestStatusLabels: Record<AdoptionRequestSummary['status'], string> = {
    PENDING: 'Pendiente',
    ACCEPTED: 'Aceptada',
    REJECTED: 'Rechazada',
    ARCHIVED: 'Archivada'
  };

  constructor(
    private fb: FormBuilder,
    private publicationService: PublicationService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.publicationEditForm = this.fb.group({
      title: ['', [Validators.required]],
      description: [''],
      adoptionStatus: [AdoptionStatus.AVAILABLE, [Validators.required]]
    });

    this.loadPublications();
  }

  goToCreatePublication(): void {
    this.router.navigate(['/posts/create']);
  }

  loadPublications(): void {
    const userId = this.getCurrentUserId();
    if (!userId) {
      this.publicationActionError = 'No se encontro el usuario autenticado.';
      return;
    }

    this.publicationService.getPublicationsByAuthor(userId).subscribe({
      next: (publications) => {
        this.publications = publications;
        this.publications.forEach(pub => {
          if (this.publicationImageIndex[pub.id] == null) {
            this.publicationImageIndex[pub.id] = 0;
          }
        });
      },
      error: () => {
        this.publicationActionError = 'No se pudieron cargar tus publicaciones.';
      }
    });
  }

  getImageUrl(url?: string): string {
    return url && url.trim().length > 0 ? url : 'assets/placeholder.png';
  }

  handleImageError(event: Event): void {
    const target = event.target as HTMLImageElement;
    target.src = 'assets/placeholder.png';
  }

  getCurrentPublicationImage(publication: PublicationSummary): string {
    const images = publication.images ?? [];
    if (images.length === 0) {
      return 'assets/placeholder.png';
    }

    const currentIndex = this.publicationImageIndex[publication.id] ?? 0;
    const safeIndex = ((currentIndex % images.length) + images.length) % images.length;
    return this.getImageUrl(images[safeIndex]?.url);
  }

  previousPublicationImage(publication: PublicationSummary): void {
    const images = publication.images ?? [];
    if (images.length <= 1) {
      return;
    }

    const current = this.publicationImageIndex[publication.id] ?? 0;
    this.publicationImageIndex[publication.id] = (current - 1 + images.length) % images.length;
  }

  nextPublicationImage(publication: PublicationSummary): void {
    const images = publication.images ?? [];
    if (images.length <= 1) {
      return;
    }

    const current = this.publicationImageIndex[publication.id] ?? 0;
    this.publicationImageIndex[publication.id] = (current + 1) % images.length;
  }

  openPublicationActions(publication: PublicationSummary): void {
    this.selectedPublication = publication;
    this.publicationModalView = 'menu';
    this.publicationActionError = '';
    this.publicationActionMessage = '';
    this.adoptionRequests = [];
    this.isPublicationModalOpen = true;

    this.publicationEditForm.patchValue({
      title: publication.title ?? '',
      description: publication.description ?? '',
      adoptionStatus: publication.adoptionStatus ?? AdoptionStatus.AVAILABLE
    });
  }

  closePublicationModal(): void {
    this.isPublicationModalOpen = false;
    this.publicationModalView = 'menu';
    this.selectedPublication = null;
    this.adoptionRequests = [];
    this.publicationActionError = '';
    this.publicationActionMessage = '';
    this.isLoadingRequests = false;
    this.isSavingPublication = false;
  }

  openEditPublication(): void {
    this.publicationModalView = 'edit';
    this.publicationActionError = '';
    this.publicationActionMessage = '';
  }

  openAdoptionRequests(): void {
    if (!this.selectedPublication) {
      return;
    }

    const userId = this.getCurrentUserId();
    if (!userId) {
      this.publicationActionError = 'No se encontro el usuario autenticado.';
      return;
    }

    this.publicationModalView = 'requests';
    this.isLoadingRequests = true;
    this.publicationActionError = '';
    this.publicationActionMessage = '';

    this.publicationService.getAdoptionRequests(this.selectedPublication.id, userId).subscribe({
      next: (requests) => {
        this.adoptionRequests = requests;
        this.isLoadingRequests = false;
      },
      error: () => {
        this.publicationActionError = 'No se pudieron cargar las solicitudes de adopcion.';
        this.isLoadingRequests = false;
      }
    });
  }

  savePublicationChanges(): void {
    if (!this.selectedPublication || this.publicationEditForm.invalid) {
      return;
    }

    const userId = this.getCurrentUserId();
    if (!userId) {
      this.publicationActionError = 'No se encontro el usuario autenticado.';
      return;
    }

    this.isSavingPublication = true;
    this.publicationActionError = '';
    this.publicationActionMessage = '';

    const payload = {
      title: String(this.publicationEditForm.get('title')?.value ?? '').trim(),
      description: String(this.publicationEditForm.get('description')?.value ?? '').trim(),
      adoptionStatus: this.publicationEditForm.get('adoptionStatus')?.value as 'AVAILABLE' | 'URGENT' | 'ADOPTED'
    };

    this.publicationService.updatePublication(this.selectedPublication.id, userId, payload).subscribe({
      next: (updatedPublication) => {
        this.selectedPublication = updatedPublication;
        this.publicationActionMessage = 'Publicacion actualizada correctamente.';
        this.isSavingPublication = false;
        this.loadPublications();
      },
      error: () => {
        this.publicationActionError = 'No se pudo actualizar la publicacion.';
        this.isSavingPublication = false;
      }
    });
  }

  updateRequestStatus(requestId: number, status: AdoptionRequestSummary['status']): void {
    if (!this.selectedPublication) {
      return;
    }

    const userId = this.getCurrentUserId();
    if (!userId) {
      this.publicationActionError = 'No se encontro el usuario autenticado.';
      return;
    }

    this.publicationActionError = '';
    this.publicationActionMessage = '';

    this.publicationService
      .updateAdoptionRequestStatus(this.selectedPublication.id, requestId, userId, status)
      .subscribe({
        next: () => {
          if (status === 'ACCEPTED') {
            const shouldDelete = window.confirm('La solicitud fue aceptada. ¿Quieres borrar la publicación ahora?');
            if (shouldDelete) {
              this.deletePublication(this.selectedPublication!.id, true);
              return;
            }
          }

          this.publicationActionMessage = 'Solicitud actualizada correctamente.';
          this.openAdoptionRequests();
          this.loadPublications();
        },
        error: () => {
          this.publicationActionError = 'No se pudo actualizar la solicitud.';
        }
      });
  }

  deleteRequest(requestId: number): void {
    if (!this.selectedPublication) {
      return;
    }

    const userId = this.getCurrentUserId();
    if (!userId) {
      this.publicationActionError = 'No se encontro el usuario autenticado.';
      return;
    }

    this.publicationActionError = '';
    this.publicationActionMessage = '';

    this.publicationService
      .deleteAdoptionRequest(this.selectedPublication.id, requestId, userId)
      .subscribe({
        next: () => {
          this.publicationActionMessage = 'Solicitud eliminada.';
          this.openAdoptionRequests();
          this.loadPublications();
        },
        error: () => {
          this.publicationActionError = 'No se pudo eliminar la solicitud.';
        }
      });
  }

  deletePublication(publicationId: number, fromAcceptedFlow = false): void {
    const userId = this.getCurrentUserId();
    if (!userId) {
      this.publicationActionError = 'No se encontro el usuario autenticado.';
      return;
    }

    if (!fromAcceptedFlow) {
      const confirmed = window.confirm('¿Seguro que quieres borrar esta publicación? Esta acción no se puede deshacer.');
      if (!confirmed) {
        return;
      }
    }

    this.publicationService.deletePublication(publicationId, userId).subscribe({
      next: () => {
        this.publicationActionMessage = 'Publicación borrada correctamente.';
        this.closePublicationModal();
        this.loadPublications();
      },
      error: () => {
        this.publicationActionError = 'No se pudo borrar la publicación.';
      }
    });
  }

  formatApplicantName(request: AdoptionRequestSummary): string {
    const firstName = request.applicant?.firstName?.trim() ?? '';
    const lastName = request.applicant?.lastName?.trim() ?? '';
    const fullName = `${firstName} ${lastName}`.trim();
    return fullName || 'Usuario sin nombre';
  }

  getApplicantPhone(request: AdoptionRequestSummary): string {
    const phone = request.applicant?.contactPhone?.trim();
    return phone && phone.length > 0 ? phone : 'Ninguno';
  }

  private getCurrentUserId(): number | null {
    const userJson = localStorage.getItem('user');
    if (!userJson) {
      return null;
    }

    const user = JSON.parse(userJson) as AuthUser;
    return user.id ?? null;
  }
}
