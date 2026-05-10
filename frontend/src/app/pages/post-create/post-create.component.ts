import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdoptionStatus } from '../../enums/adoption-status';
import { PetService } from '../../services/pet/pet.service';
import { PublicationService } from '../../services/publication/publication.service';

interface PetImage {
  url: string;
}

interface PetSummary {
  id: number;
  name: string;
  species?: { name?: string };
  breed?: { name?: string };
  images?: PetImage[];
}

interface PreviewImage {
  url: string;
  name: string;
  source: 'pet' | 'upload';
}

@Component({
  selector: 'app-post-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './post-create.component.html',
  styleUrl: './post-create.component.css'
})
export class PostCreateComponent implements OnInit {
  postForm!: FormGroup;
  pets: PetSummary[] = [];
  selectedPetIds: number[] = [];
  selectedFiles: File[] = [];
  previewImages: PreviewImage[] = [];
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';
  adoptionStatuses = Object.values(AdoptionStatus);
  readonly statusLabels: Record<AdoptionStatus, string> = {
    [AdoptionStatus.AVAILABLE]: 'Se puede adoptar',
    [AdoptionStatus.URGENT]: 'Urgente de adopcion',
    [AdoptionStatus.ADOPTED]: 'Adoptado'
  };

  constructor(
    private fb: FormBuilder,
    private petService: PetService,
    private publicationService: PublicationService
  ) { }

  ngOnInit(): void {
    this.postForm = this.fb.group({
      title: [''],
      description: [''],
      adoptionStatus: [AdoptionStatus.AVAILABLE, [Validators.required]]
    });

    this.loadPets();
  }

  get titlePlaceholder(): string {
    if (this.selectedPetIds.length === 0) {
      return 'Nombre de la publicacion';
    }

    const fallbackNames = this.pets
      .filter(p => this.selectedPetIds.includes(p.id))
      .map(p => p.name)
      .join(', ');

    return fallbackNames || 'Nombre de la publicacion';
  }

  get selectedPetErrorVisible(): boolean {
    return this.selectedPetIds.length === 0 && this.isSubmitting;
  }

  loadPets(): void {
    const userJson = localStorage.getItem('user');
    if (!userJson) {
      this.errorMessage = 'Inicia sesion para crear una publicacion.';
      return;
    }

    const user = JSON.parse(userJson) as { id?: number };
    if (!user.id) {
      this.errorMessage = 'No se encontro el usuario autenticado.';
      return;
    }

    this.petService.getPetsByOwner(user.id).subscribe({
      next: (pets) => {
        this.pets = pets;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar tus mascotas.';
      }
    });
  }

  togglePetSelection(petId: number, checked: boolean): void {
    if (checked) {
      if (!this.selectedPetIds.includes(petId)) {
        this.selectedPetIds = [...this.selectedPetIds, petId];
      }
    } else {
      this.selectedPetIds = this.selectedPetIds.filter(id => id !== petId);
    }

    this.syncPetImagePreviews();
  }

  onImagesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }

    const files = Array.from(input.files);
    this.selectedFiles = [...this.selectedFiles, ...files];

    files.forEach(file => {
      const reader = new FileReader();
      reader.onload = () => {
        this.previewImages = [
          ...this.previewImages,
          {
            url: reader.result as string,
            name: file.name,
            source: 'upload'
          }
        ];
      };
      reader.readAsDataURL(file);
    });

    input.value = '';
  }

  removeUploadedImage(imageToRemove: PreviewImage): void {
    if (!imageToRemove || imageToRemove.source !== 'upload') {
      return;
    }

    this.previewImages = this.previewImages.filter(img => img !== imageToRemove);
    const fileIndex = this.selectedFiles.findIndex(file => file.name === imageToRemove.name);
    if (fileIndex >= 0) {
      this.selectedFiles.splice(fileIndex, 1);
      this.selectedFiles = [...this.selectedFiles];
    }
  }

  createPublication(): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.isSubmitting = true;

    if (this.postForm.invalid || this.selectedPetIds.length === 0) {
      this.errorMessage = this.selectedPetIds.length === 0
        ? 'Debes asociar al menos una mascota.'
        : 'Revisa los campos del formulario.';
      this.isSubmitting = false;
      return;
    }

    const userJson = localStorage.getItem('user');
    if (!userJson) {
      this.errorMessage = 'Inicia sesion para crear una publicacion.';
      this.isSubmitting = false;
      return;
    }

    const user = JSON.parse(userJson) as { id?: number };
    if (!user.id) {
      this.errorMessage = 'No se encontro el usuario autenticado.';
      this.isSubmitting = false;
      return;
    }

    const titleRaw = (this.postForm.get('title')?.value as string | null)?.trim() ?? '';
    const descriptionRaw = (this.postForm.get('description')?.value as string | null)?.trim() ?? '';

    const payload = {
      title: titleRaw || null,
      description: descriptionRaw || '',
      adoptionStatus: this.postForm.get('adoptionStatus')?.value as AdoptionStatus,
      petIds: this.selectedPetIds
    };

    const formData = new FormData();
    formData.append('publication', JSON.stringify(payload));
    this.selectedFiles.forEach(file => formData.append('images', file));

    this.publicationService.createPublication(formData, user.id).subscribe({
      next: () => {
        this.successMessage = 'Publicacion creada correctamente.';
        this.resetForm();
      },
      error: () => {
        this.errorMessage = 'No se pudo crear la publicacion. Intenta de nuevo.';
        this.isSubmitting = false;
      }
    });
  }

  private syncPetImagePreviews(): void {
    const petImages: PreviewImage[] = [];

    this.pets
      .filter(pet => this.selectedPetIds.includes(pet.id))
      .forEach(pet => {
        (pet.images ?? []).forEach((img, index) => {
          if (!img.url) {
            return;
          }

          petImages.push({
            url: img.url,
            name: `${pet.name}-${index + 1}`,
            source: 'pet'
          });
        });
      });

    const uploaded = this.previewImages.filter(img => img.source === 'upload');
    this.previewImages = [...petImages, ...uploaded];
  }

  private resetForm(): void {
    this.postForm.reset({
      title: '',
      description: '',
      adoptionStatus: AdoptionStatus.AVAILABLE
    });
    this.selectedPetIds = [];
    this.selectedFiles = [];
    this.previewImages = [];
    this.isSubmitting = false;
  }

}
