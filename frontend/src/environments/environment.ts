export const environment = {
  production: false,
  //apiBase: 'http://localhost:8000/api'
  apiBaseConsulta: '/api/consulta',                        // <- en vez de http://localhost:8000
  apiBaseComando: '/api/comando',
  keycloak: {
    url: `${window.location.origin}/auth`,
    realm: 'arquitecturaTI',
    clientId: 'angular-app'
  }
};
