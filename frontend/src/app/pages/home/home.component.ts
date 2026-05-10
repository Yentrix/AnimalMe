import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PublicationService, PublicationSummary } from '../../services/publication/publication.service';

interface AuthUser {
  id?: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  publications: PublicationSummary[] = [];
  publicationImageIndex: Record<number, number> = {};
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  isLightboxOpen = false;
  selectedPublication: PublicationSummary | null = null;
  selectedModalImageIndex = 0;
  requestMessage = '';
  isSubmittingRequest = false;

  readonly adoptionStatusLabel: Record<'AVAILABLE' | 'URGENT' | 'ADOPTED', string> = {
    AVAILABLE: 'Disponible',
    URGENT: 'Urgente',
    ADOPTED: 'Adoptado'
  };

  constructor(private publicationService: PublicationService) { }

  ngOnInit(): void {
    this.loadPublications();
  }

  loadPublications(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.publicationService.getActivePublications().subscribe({
      next: (publications) => {
        this.publications = publications;
        this.publications.forEach(pub => {
          if (this.publicationImageIndex[pub.id] == null) {
            this.publicationImageIndex[pub.id] = 0;
          }
        });
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar las publicaciones disponibles.';
        this.isLoading = false;
      }
    });
  }

  getCurrentImage(publication: PublicationSummary): string {
    const images = publication.images ?? [];
    if (images.length === 0) {
      return 'assets/placeholder.png';
    }

    const index = this.publicationImageIndex[publication.id] ?? 0;
    const safeIndex = ((index % images.length) + images.length) % images.length;
    return images[safeIndex]?.url || 'assets/placeholder.png';
  }

  previousImage(publication: PublicationSummary): void {
    const images = publication.images ?? [];
    if (images.length <= 1) {
      return;
    }

    const index = this.publicationImageIndex[publication.id] ?? 0;
    this.publicationImageIndex[publication.id] = (index - 1 + images.length) % images.length;
  }

  nextImage(publication: PublicationSummary): void {
    const images = publication.images ?? [];
    if (images.length <= 1) {
      return;
    }

    const index = this.publicationImageIndex[publication.id] ?? 0;
    this.publicationImageIndex[publication.id] = (index + 1) % images.length;
  }

  openLightbox(publication: PublicationSummary): void {
    this.selectedPublication = publication;
    this.selectedModalImageIndex = this.publicationImageIndex[publication.id] ?? 0;
    this.requestMessage = '';
    this.successMessage = '';
    this.errorMessage = '';
    this.isLightboxOpen = true;
  }

  closeLightbox(): void {
    this.isLightboxOpen = false;
    this.selectedPublication = null;
    this.selectedModalImageIndex = 0;
    this.requestMessage = '';
    this.isSubmittingRequest = false;
  }

  previousModalImage(): void {
    const images = this.selectedPublication?.images ?? [];
    if (images.length <= 1) {
      return;
    }

    this.selectedModalImageIndex = (this.selectedModalImageIndex - 1 + images.length) % images.length;
  }

  nextModalImage(): void {
    const images = this.selectedPublication?.images ?? [];
    if (images.length <= 1) {
      return;
    }

    this.selectedModalImageIndex = (this.selectedModalImageIndex + 1) % images.length;
  }

  getModalImage(): string {
    const images = this.selectedPublication?.images ?? [];
    if (images.length === 0) {
      return 'assets/placeholder.png';
    }

    const safeIndex = ((this.selectedModalImageIndex % images.length) + images.length) % images.length;
    return images[safeIndex]?.url || 'assets/placeholder.png';
  }

  submitAdoptionRequest(): void {
    if (!this.selectedPublication) {
      return;
    }

    const userId = this.getCurrentUserId();
    if (!userId) {
      this.errorMessage = 'Inicia sesion para enviar una solicitud de adopcion.';
      return;
    }

    if (this.isPublicationOwner(this.selectedPublication)) {
      this.errorMessage = 'No puedes solicitar adopcion de tu propia publicacion.';
      return;
    }

    this.isSubmittingRequest = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.publicationService
      .createAdoptionRequest(this.selectedPublication.id, userId, this.requestMessage.trim())
      .subscribe({
        next: () => {
          this.successMessage = 'Solicitud enviada correctamente.';
          this.requestMessage = '';
          this.isSubmittingRequest = false;
        },
        error: () => {
          this.errorMessage = 'No se pudo enviar la solicitud. Revisa si ya tienes una pendiente.';
          this.isSubmittingRequest = false;
        }
      });
  }

  isPublicationOwner(publication: PublicationSummary): boolean {
    const userId = this.getCurrentUserId();
    return !!userId && publication.author?.id === userId;
  }

  hasSession(): boolean {
    return this.getCurrentUserId() != null;
  }

  getAuthorName(publication: PublicationSummary): string {
    const firstName = publication.author?.firstName?.trim() ?? '';
    const lastName = publication.author?.lastName?.trim() ?? '';
    const fullName = `${firstName} ${lastName}`.trim();
    return fullName || publication.author?.email || 'Usuario';
  }

  handleImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'assets/placeholder.png';
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
