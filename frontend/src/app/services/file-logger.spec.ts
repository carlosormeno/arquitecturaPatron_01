import { TestBed } from '@angular/core/testing';

import { FileLogger } from './file-logger';

describe('FileLogger', () => {
  let service: FileLogger;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FileLogger);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
