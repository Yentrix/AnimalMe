import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { PublicationPet, PublicationService, PublicationSummary } from '../../services/publication/publication.service';
import { svgIcons } from '../../icons/svg-icons';

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
  allPublications: PublicationSummary[] = [];
  publications: PublicationSummary[] = [];
  searchPetName = '';
  selectedSpecies = '';
  selectedBreed = '';
  searchDraftPetName = '';
  selectedSpeciesDraft = '';
  selectedBreedDraft = '';
  showOnlyFavorites = false;
  favoritePublicationIds = new Set<number>();
  speciesOptions: string[] = [];
  breedOptions: string[] = [];
  publicationImageIndex: Record<number, number> = {};
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  sanitizedIcons: Record<keyof typeof svgIcons, SafeHtml>;

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

  constructor(
    private publicationService: PublicationService,
    private sanitizer: DomSanitizer
  ) {
    this.sanitizedIcons = {} as Record<keyof typeof svgIcons, SafeHtml>;
    for (const key of Object.keys(svgIcons) as Array<keyof typeof svgIcons>) {
      this.sanitizedIcons[key] = this.sanitizer.bypassSecurityTrustHtml(svgIcons[key]);
    }
  }

  ngOnInit(): void {
    this.loadFavoritePublications();
    this.loadPublications();
  }

  loadPublications(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.publicationService.getActivePublications().subscribe({
      next: (publications) => {
        this.allPublications = publications;
        this.allPublications.forEach(pub => {
          if (this.publicationImageIndex[pub.id] == null) {
            this.publicationImageIndex[pub.id] = 0;
          }
        });

        this.speciesOptions = this.extractSpeciesOptions(this.allPublications);
        this.refreshBreedOptions();
        this.applyFilters();
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar las publicaciones disponibles.';
        this.isLoading = false;
      }
    });
  }

  applySearchFilters(): void {
    this.searchPetName = this.searchDraftPetName;
    this.selectedSpecies = this.selectedSpeciesDraft;
    this.selectedBreed = this.selectedBreedDraft;
    this.applyFilters();
  }

  applyFilters(): void {
    const nameNeedle = this.searchPetName.trim().toLowerCase();
    const speciesNeedle = this.selectedSpecies.trim().toLowerCase();
    const breedNeedle = this.selectedBreed.trim().toLowerCase();

    this.publications = this.allPublications.filter(publication => {
      if (this.showOnlyFavorites && !this.favoritePublicationIds.has(publication.id)) {
        return false;
      }

      const pets = publication.pets ?? [];
      return pets.some(pet => {
        const petName = (pet.name ?? '').toLowerCase();
        const petSpecies = (pet.species?.name ?? '').toLowerCase();
        const petBreed = (pet.breed?.name ?? '').toLowerCase();

        const matchesName = nameNeedle.length === 0 || petName.includes(nameNeedle);
        const matchesSpecies = speciesNeedle.length === 0 || petSpecies.includes(speciesNeedle);
        const matchesBreed = breedNeedle.length === 0 || petBreed.includes(breedNeedle);

        return matchesName && matchesSpecies && matchesBreed;
      });
    });
  }

  onSpeciesChange(): void {
    this.selectedSpeciesDraft = this.selectedSpeciesDraft.trim();
    this.refreshBreedOptions();
    if (this.selectedBreedDraft && !this.breedOptions.includes(this.selectedBreedDraft)) {
      this.selectedBreedDraft = '';
    }
  }

  onBreedChange(): void {
    this.selectedBreedDraft = this.selectedBreedDraft.trim();
  }

  clearFilters(): void {
    this.searchPetName = '';
    this.selectedSpecies = '';
    this.selectedBreed = '';
    this.searchDraftPetName = '';
    this.selectedSpeciesDraft = '';
    this.selectedBreedDraft = '';
    this.showOnlyFavorites = false;
    this.refreshBreedOptions();
    this.applyFilters();
  }

  toggleFavoritesFilter(): void {
    this.showOnlyFavorites = !this.showOnlyFavorites;
    this.applyFilters();
  }

  isFavorite(publication: PublicationSummary): boolean {
    return this.favoritePublicationIds.has(publication.id);
  }

  toggleFavoritePublication(publication: PublicationSummary, event?: Event): void {
    event?.stopPropagation();

    if (this.isPublicationOwner(publication) || !this.hasSession()) {
      return;
    }

    const userId = this.getCurrentUserId();
    if (!userId) {
      return;
    }

    if (this.favoritePublicationIds.has(publication.id)) {
      this.favoritePublicationIds.delete(publication.id);
    } else {
      this.favoritePublicationIds.add(publication.id);
    }

    this.persistFavoritePublications();

    if (this.showOnlyFavorites) {
      this.applyFilters();
    }
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

  setModalImage(index: number): void {
    this.selectedModalImageIndex = index;
  }

  getPetImage(pet: PublicationPet): string {
    const firstImage = pet.images?.[0]?.url;
    return firstImage && firstImage.trim().length > 0 ? firstImage : 'assets/placeholder.png';
  }

  getPetDetailRows(publication: PublicationSummary): PublicationPet[] {
    return publication.pets ?? [];
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

  private getFavoriteStorageKey(userId: number): string {
    return `animalme:favorites:${userId}`;
  }

  private loadFavoritePublications(): void {
    const userId = this.getCurrentUserId();
    if (!userId) {
      this.favoritePublicationIds = new Set<number>();
      return;
    }

    const stored = localStorage.getItem(this.getFavoriteStorageKey(userId));
    if (!stored) {
      this.favoritePublicationIds = new Set<number>();
      return;
    }

    try {
      const parsed = JSON.parse(stored) as number[];
      this.favoritePublicationIds = new Set<number>(parsed.filter(id => Number.isFinite(id)));
    } catch {
      this.favoritePublicationIds = new Set<number>();
    }
  }

  private persistFavoritePublications(): void {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return;
    }

    localStorage.setItem(this.getFavoriteStorageKey(userId), JSON.stringify(Array.from(this.favoritePublicationIds)));
  }

  private extractSpeciesOptions(publications: PublicationSummary[]): string[] {
    const speciesSet = new Set<string>();
    publications.forEach(publication => {
      (publication.pets ?? []).forEach(pet => {
        const name = pet.species?.name?.trim();
        if (name) {
          speciesSet.add(name);
        }
      });
    });
    return Array.from(speciesSet).sort((a, b) => a.localeCompare(b));
  }

  private refreshBreedOptions(): void {
    const speciesNeedle = this.selectedSpeciesDraft.trim().toLowerCase();
    const breedSet = new Set<string>();

    this.allPublications.forEach(publication => {
      (publication.pets ?? []).forEach(pet => {
        const speciesName = (pet.species?.name ?? '').toLowerCase();
        const breedName = pet.breed?.name?.trim();

        if (!breedName) {
          return;
        }

        if (!speciesNeedle || speciesName.includes(speciesNeedle)) {
          breedSet.add(breedName);
        }
      });
    });

    this.breedOptions = Array.from(breedSet).sort((a, b) => a.localeCompare(b));
  }

}
