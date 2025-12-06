import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LinksManager } from './links-manager';

describe('LinksManager', () => {
  let component: LinksManager;
  let fixture: ComponentFixture<LinksManager>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LinksManager]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LinksManager);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
