import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';

import { Forbidden } from './forbidden';

describe('Forbidden', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Forbidden],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('should create the page', () => {
    const fixture = TestBed.createComponent(Forbidden);

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should explain that access was denied', () => {
    const fixture = TestBed.createComponent(Forbidden);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Access denied');
  });
});
