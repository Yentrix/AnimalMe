import { Component, OnDestroy, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'animalme';

  private observer: MutationObserver | null = null;

  ngOnInit(): void {
    this.syncBodyScrollLock();

    this.observer = new MutationObserver(() => {
      this.syncBodyScrollLock();
    });

    this.observer.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['class', 'style']
    });
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
    this.observer = null;
    document.body.classList.remove('modal-open');
  }

  private syncBodyScrollLock(): void {
    const activeOverlays = document.querySelectorAll('.modal-overlay, .modal-backdrop, .ban-modal-backdrop').length;
    if (activeOverlays > 0) {
      document.body.classList.add('modal-open');
      return;
    }

    document.body.classList.remove('modal-open');
  }
}
