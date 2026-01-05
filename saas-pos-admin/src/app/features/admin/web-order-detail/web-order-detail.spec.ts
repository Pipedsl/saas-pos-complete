import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WebOrderDetail } from './web-order-detail';

describe('WebOrderDetail', () => {
  let component: WebOrderDetail;
  let fixture: ComponentFixture<WebOrderDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebOrderDetail]
    })
    .compileComponents();

    fixture = TestBed.createComponent(WebOrderDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
