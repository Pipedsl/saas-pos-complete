import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StoreLayout } from './store-layout';

describe('StoreLayout', () => {
  let component: StoreLayout;
  let fixture: ComponentFixture<StoreLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StoreLayout]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StoreLayout);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
