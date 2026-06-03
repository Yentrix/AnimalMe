import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { PetService } from '../../services/pet/pet.service';
import { PublicationService } from '../../services/publication/publication.service';
import { svgIcons } from '../../icons/svg-icons';

@Component({
  selector: 'app-pet-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './pet-create.component.html',
  styleUrl: './pet-create.component.css'
})
export class PetManagementComponent implements OnInit {
  readonly petDescriptionMaxLength = 255;
  readonly maxImageUploadBytes = 900 * 1024;
  readonly maxImageDimension = 1600;
  viewMode: 'list' | 'kanban' = 'kanban';
  imagePreview: string | null = null;
  imageUploadError = '';
  pets: any[] = [];
  petForm!: FormGroup;
  selectedFile: File | null = null;
  showModal = false;
  isEditing = false;
  selectedPetId: number | null = null;
  filteredSpecies: any[] = [];
  filteredBreeds: any[] = [];
  allSpecies: any[] = [];
  allBreeds: any[] = [];
  petsInPublication = new Set<number>();
  iconPencil: SafeHtml;
  ageInput = '';
  sizeCmInput = '';


  constructor(
    private fb: FormBuilder,
    private petService: PetService,
    private publicationService: PublicationService,
    private sanitizer: DomSanitizer
  ) {
    this.iconPencil = this.sanitizer.bypassSecurityTrustHtml(svgIcons.ICON_EDIT_PENCIL);
  }

  ngOnInit(): void {
    this.initForm();
    this.loadPets();
  }

  initForm() {
    this.petForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60)]],
      // Los demás son opcionales (sin Validators.required)
      age: [null, [Validators.min(0), Validators.max(40)]],
      sex: [''],
      sizeCm: [null, [Validators.min(1), Validators.max(250)]],
      description: ['', [Validators.maxLength(this.petDescriptionMaxLength)]],
      speciesName: [''],
      breedName: [{ value: '', disabled: true }]
    });

    // Escuchar cambios en especie para habilitar raza
    this.petForm.get('speciesName')?.valueChanges.subscribe(value => {
      const breedControl = this.petForm.get('breedName');
      if (value && value.trim().length > 0) {
        breedControl?.enable();
      } else {
        breedControl?.disable();
        breedControl?.setValue('');
      }
    });
  }

  loadPets() {
    // Obtenemos el usuario del localStorage (el que guardamos en el Login)
    const userJson = localStorage.getItem('user');
    if (!userJson) return;

    const user = JSON.parse(userJson);
    const ownerId = user.id;

    this.petService.getPetsByOwner(ownerId).subscribe({
      next: (data) => {
        this.pets = data; // Asignamos el array de mascotas que viene del servidor
        this.loadPetsInPublication(ownerId);
        console.log('Mascotas cargadas con éxito:', this.pets);
      },
      error: (err) => {
        console.error('Error al cargar las mascotas:', err);
      }
    });
  }

  loadPetsInPublication(ownerId: number) {
    this.publicationService.getPublicationsByAuthor(ownerId).subscribe({
      next: (publications) => {
        const associated = new Set<number>();
        publications.forEach(pub => {
          (pub.pets ?? []).forEach(pet => associated.add(pet.id));
        });
        this.petsInPublication = associated;
      },
      error: () => {
        this.petsInPublication = new Set<number>();
      }
    });
  }

  editPet(pet: any) {
    this.isEditing = true;
    this.selectedPetId = pet.id;
    this.showModal = true;

    // Rellenamos el formulario con los datos actuales
    this.petForm.patchValue({
      name: pet.name,
      age: pet.age,
      sex: pet.sex,
      sizeCm: pet.sizeCm,
      description: pet.description,
      speciesName: pet.species?.name,
      breedName: pet.breed?.name
    });
    this.ageInput = this.formatWithUnit(pet.age, 'años');
    this.sizeCmInput = this.formatWithUnit(pet.sizeCm, 'cm');

    // Si la mascota tiene imagen, mostramos la preview
    this.imagePreview = this.getPetMainImageUrl(pet);
  }

  getPetMainImageUrl(pet: any): string | null {
    if (!pet?.images || pet.images.length === 0) {
      return null;
    }

    const lastImage = pet.images[pet.images.length - 1];
    return lastImage?.url ?? null;
  }

  selectSpecies(species: any) {
    this.petForm.patchValue({
      speciesName: species.name
    });
    this.filteredSpecies = []; // Limpiamos las sugerencias

    // Opcional: Si quieres cargar las razas de esa especie inmediatamente
    // this.loadBreedsBySpecies(species.id); 
  }

  selectBreed(breed: any) {
    this.petForm.patchValue({
      breedName: breed.name
    });
    this.filteredBreeds = []; // Cerramos la lista
  }

  async onFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.imageUploadError = '';

    if (!file.type.startsWith('image/')) {
      this.selectedFile = null;
      this.imageUploadError = 'El archivo seleccionado no es una imagen valida.';
      input.value = '';
      return;
    }

    try {
      const optimizedFile = await this.optimizeImageForUpload(file);

      if (optimizedFile.size > this.maxImageUploadBytes) {
        this.selectedFile = null;
        this.imageUploadError = 'La imagen es demasiado grande. Prueba con una mas ligera.';
        input.value = '';
        return;
      }

      this.selectedFile = optimizedFile;
      this.imagePreview = await this.readAsDataUrl(optimizedFile);
    } catch (error) {
      this.selectedFile = null;
      this.imageUploadError = 'No se pudo procesar la imagen. Prueba con otro archivo.';
      input.value = '';
      console.error('Error procesando imagen:', error);
    }
  }

  savePet() {
    this.imageUploadError = '';

    const ageValue = this.parseNumberInput(this.ageInput);
    const sizeValue = this.parseNumberInput(this.sizeCmInput);

    this.petForm.patchValue({
      age: ageValue,
      sizeCm: sizeValue
    }, { emitEvent: false });

    this.petForm.get('name')?.markAsTouched();
    this.petForm.get('age')?.markAsTouched();
    this.petForm.get('sizeCm')?.markAsTouched();
    this.petForm.updateValueAndValidity();

    if (this.petForm.invalid) {
      return;
    }

    const formData = new FormData();
    const petData = {
      name: this.petForm.get('name')?.value,
      age: ageValue,
      sex: this.petForm.get('sex')?.value,
      sizeCm: sizeValue,
      description: this.petForm.get('description')?.value
    };

    formData.append('pet', JSON.stringify(petData));
    formData.append('speciesName', this.petForm.get('speciesName')?.value);
    formData.append('breedName', this.petForm.get('breedName')?.value);

    if (this.selectedFile) {
      formData.append('image', this.selectedFile);
    }

    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const ownerId = user.id;

    if (this.isEditing && this.selectedPetId) {
      // Actualizar
      this.petService.updatePet(this.selectedPetId, formData).subscribe({
        next: () => this.handleSuccess(),
        error: (err) => {
          if (err?.status === 413) {
            this.imageUploadError = 'La imagen supera el limite permitido por el servidor.';
          }
          console.error('Error al actualizar:', err);
        }
      });
    } else {
      // Crear
      this.petService.createPet(formData, ownerId).subscribe({
        next: () => this.handleSuccess(),
        error: (err) => {
          if (err?.status === 413) {
            this.imageUploadError = 'La imagen supera el limite permitido por el servidor.';
          }
          console.error('Error al crear:', err);
        }
      });
    }
  }

  handleSuccess() {
    this.closeModal();
    this.loadPets();
  }

  handleImageError(event: any) {
    event.target.src = 'assets/placeholder.png';
  }

  closeModal() {
    this.showModal = false;
    this.isEditing = false;
    this.selectedPetId = null;
    this.imagePreview = null;
    this.imageUploadError = '';
    this.selectedFile = null;
    this.ageInput = '';
    this.sizeCmInput = '';
    this.petForm.reset();
  }

  onAgeFocus(): void {
    const value = this.parseNumberInput(this.ageInput);
    this.ageInput = value == null ? '' : `${value}`;
  }

  onAgeBlur(): void {
    const value = this.parseNumberInput(this.ageInput);
    this.petForm.patchValue({ age: value }, { emitEvent: false });
    this.ageInput = this.formatWithUnit(value, 'años');
  }

  onAgeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const sanitized = this.sanitizeNumericInput(input.value);
    const value = this.parseNumberInput(sanitized);
    this.petForm.patchValue({ age: value }, { emitEvent: false });
    this.ageInput = sanitized;
  }

  onSizeCmFocus(): void {
    const value = this.parseNumberInput(this.sizeCmInput);
    this.sizeCmInput = value == null ? '' : `${value}`;
  }

  onSizeCmBlur(): void {
    const value = this.parseNumberInput(this.sizeCmInput);
    this.petForm.patchValue({ sizeCm: value }, { emitEvent: false });
    this.sizeCmInput = this.formatWithUnit(value, 'cm');
  }

  onSizeCmInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const sanitized = this.sanitizeNumericInput(input.value);
    const value = this.parseNumberInput(sanitized);
    this.petForm.patchValue({ sizeCm: value }, { emitEvent: false });
    this.sizeCmInput = sanitized;
  }

  onSpeciesSearch(event: any) {
    const query = event.target.value;
    if (query.length >= 3) {
      const cleanQuery = this.normalizeQuery(query);
      this.filteredSpecies = this.allSpecies.filter(s =>
        this.normalizeQuery(s.name).includes(cleanQuery)
      );
    } else {
      this.filteredSpecies = [];
    }
  }

  onBreedSearch(event: any) {
    const query = event.target.value;

    if (query.length >= 3) {
      const cleanQuery = this.normalizeQuery(query);

      // Filtramos las razas que coincidan con el texto
      this.filteredBreeds = this.allBreeds.filter(breed => {
        const cleanBreedName = this.normalizeQuery(breed.name);

        // Opcional: Si quieres que solo muestre razas de la especie seleccionada
        const currentSpecies = this.normalizeQuery(this.petForm.get('speciesName')?.value);
        const matchesSpecies = currentSpecies ? this.normalizeQuery(breed.species?.name).includes(currentSpecies) : true;

        return cleanBreedName.includes(cleanQuery) && matchesSpecies;
      });
    } else {
      this.filteredBreeds = [];
    }
  }

  normalizeQuery(text: string): string {
    if (!text) return '';
    return text.normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "") // Quita acentos
      .replace(/[^a-zA-Z0-9 ]/g, "")    // Quita símbolos
      .toLowerCase()
      .trim();
  }

  private parseNumberInput(value: unknown): number | null {
    if (value == null) {
      return null;
    }

    const text = String(value).trim();
    if (text.length === 0) {
      return null;
    }

    const match = text.match(/\d+/);
    if (!match) {
      return null;
    }

    const parsed = Number(match[0]);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private sanitizeNumericInput(value: string): string {
    return value.replace(/[^\d]/g, '').slice(0, 3);
  }

  private formatWithUnit(value: unknown, unit: string): string {
    const numberValue = this.parseNumberInput(value);
    return numberValue == null ? '' : `${numberValue} ${unit}`;
  }

  private async optimizeImageForUpload(file: File): Promise<File> {
    if (file.size <= this.maxImageUploadBytes) {
      return file;
    }

    const dataUrl = await this.readAsDataUrl(file);
    const image = await this.loadImage(dataUrl);
    const canvas = document.createElement('canvas');

    const scaledSize = this.getScaledSize(image.width, image.height, this.maxImageDimension);
    canvas.width = scaledSize.width;
    canvas.height = scaledSize.height;

    const context = canvas.getContext('2d');
    if (!context) {
      throw new Error('No se pudo crear el contexto de imagen');
    }

    context.drawImage(image, 0, 0, scaledSize.width, scaledSize.height);

    const qualities = [0.86, 0.78, 0.7, 0.62, 0.55];
    let bestCandidate: Blob | null = null;

    for (const quality of qualities) {
      const blob = await this.canvasToJpegBlob(canvas, quality);
      if (!blob) {
        continue;
      }

      if (!bestCandidate || blob.size < bestCandidate.size) {
        bestCandidate = blob;
      }

      if (blob.size <= this.maxImageUploadBytes) {
        bestCandidate = blob;
        break;
      }
    }

    if (!bestCandidate) {
      throw new Error('No se pudo generar una imagen optimizada');
    }

    const fileName = this.toJpgFileName(file.name);
    return new File([bestCandidate], fileName, {
      type: 'image/jpeg',
      lastModified: Date.now()
    });
  }

  private toJpgFileName(originalName: string): string {
    const dotIndex = originalName.lastIndexOf('.');
    const baseName = dotIndex > 0 ? originalName.slice(0, dotIndex) : originalName;
    return `${baseName}.jpg`;
  }

  private getScaledSize(width: number, height: number, maxDimension: number): { width: number; height: number } {
    const largestDimension = Math.max(width, height);
    if (largestDimension <= maxDimension) {
      return { width, height };
    }

    const ratio = maxDimension / largestDimension;
    return {
      width: Math.max(1, Math.round(width * ratio)),
      height: Math.max(1, Math.round(height * ratio))
    };
  }

  private canvasToJpegBlob(canvas: HTMLCanvasElement, quality: number): Promise<Blob | null> {
    return new Promise(resolve => {
      canvas.toBlob(blob => resolve(blob), 'image/jpeg', quality);
    });
  }

  private readAsDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  private loadImage(dataUrl: string): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      const image = new Image();
      image.onload = () => resolve(image);
      image.onerror = () => reject(new Error('No se pudo cargar la imagen'));
      image.src = dataUrl;
    });
  }

  get nameControl() {
    return this.petForm.get('name');
  }

  get ageControl() {
    return this.petForm.get('age');
  }

  get sizeCmControl() {
    return this.petForm.get('sizeCm');
  }

  get descriptionControl() {
    return this.petForm.get('description');
  }

  get petDescriptionLength(): number {
    const value = this.descriptionControl?.value;
    return typeof value === 'string' ? value.length : 0;
  }
}
