import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateMatch } from './create-match';

describe('CreateMatch', () => {
  let component: CreateMatch;
  let fixture: ComponentFixture<CreateMatch>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateMatch],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateMatch);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
