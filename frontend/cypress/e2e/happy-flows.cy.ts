/// <reference types="cypress" />

const seedMember = 'G0002';
const onlyFlow = Cypress.env('ONLY_FLOW') as string | undefined;

function resetBrowserState() {
  cy.clearCookies();
  cy.clearLocalStorage();
  cy.visit('/login');
  cy.window().then((window) => {
    window.sessionStorage.clear();
    window.localStorage.clear();
  });
}

function assertCardOrEmptyState(cardSelector: string, emptyText: string) {
  cy.get('body').then(($body) => {
    const cards = $body.find(cardSelector);

    if (cards.length > 0) {
      expect(
        cards.toArray().some((card) => Cypress.$(card).is(':visible')),
        `${cardSelector} doit contenir au moins une carte visible`,
      ).to.eq(true);
      return;
    }

    cy.contains(emptyText).should('be.visible');
  });
}

function flow(key: string, title: string, testFn: Mocha.Func) {
  if (!onlyFlow || onlyFlow === key) {
    it(title, testFn);
  }
}

describe('happy flows principaux', () => {
  beforeEach(() => {
    resetBrowserState();
  });

  flow('login', 'connecte un membre et arrive sur l accueil', () => {
    // Arrange / Act
    cy.loginByUi(seedMember);

    // Assert
    cy.location('pathname').should('eq', '/home');
  });

  flow('matchs', 'consulte la page des matchs publics', () => {
    // Arrange
    cy.loginByUi(seedMember);
    cy.intercept('GET', '/api/match/disponibles').as('matchsDisponibles');
    cy.intercept('GET', `/api/reservation/membre/${seedMember}`).as('reservationsMembre');

    // Act
    cy.visit('/match');

    // Assert
    cy.wait('@matchsDisponibles').its('response.statusCode').should('eq', 200);
    cy.wait('@reservationsMembre').its('response.statusCode').should('eq', 200);
    cy.contains('Matchs publics disponibles').should('be.visible');
    assertCardOrEmptyState('[data-cy="match-card"]', 'Aucun match public n');
  });

  flow('reservations', 'consulte les reservations du membre du seed', () => {
    // Arrange
    cy.loginByUi(seedMember);
    cy.intercept('GET', `/api/reservation/membre/${seedMember}`).as('reservationsMembre');

    // Act
    cy.visit('/mes-reservations');

    // Assert
    cy.wait('@reservationsMembre').its('response.statusCode').should('eq', 200);
    cy.contains('Mes r').should('be.visible');
    assertCardOrEmptyState('[data-cy="reservation-card"]', 'Vous n');
  });
});
