import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WebOrders } from './web-orders';

describe('WebOrders', () => {
  let component: WebOrders;
  let fixture: ComponentFixture<WebOrders>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebOrders]
    })
    .compileComponents();

    fixture = TestBed.createComponent(WebOrders);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
