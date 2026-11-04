import { TestBed } from '@angular/core/testing';

import { Protected } from './protected';

describe('Protected', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Protected],
    }).compileComponents();
  });

  it('should create the page', () => {
    const fixture = TestBed.createComponent(Protected);

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should describe the protected area', () => {
    const fixture = TestBed.createComponent(Protected);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Protected area');
  });
});
