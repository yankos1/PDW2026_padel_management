/// <reference types="cypress" />

declare global {
  namespace Cypress {
    interface Chainable {
      loginByUi(matricule: string): Chainable<void>;
    }
  }
}

Cypress.Commands.add('loginByUi', (matricule: string) => {
  cy.intercept('POST', '/api/auth/login').as('login');

  cy.visit('/login');
  cy.get('[data-cy="login-matricule"]').clear().type(matricule);
  cy.get('[data-cy="login-submit"]').click();
  cy.wait('@login').its('response.statusCode').should('eq', 200);
  cy.location('pathname').should('eq', '/home');
});

export {};
