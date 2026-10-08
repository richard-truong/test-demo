// Thin wrapper around the user REST API.
// Every call is tagged with `op`, so the summary can break response times
// down per operation instead of showing one blended number.
import http from 'k6/http';
import { ENDPOINTS, JSON_HEADERS } from './config.js';

export function createUser(user) {
  return http.post(ENDPOINTS.users, JSON.stringify(user), {
    headers: JSON_HEADERS,
    tags: { op: 'create_user' },
  });
}

export function getUser(id) {
  return http.get(ENDPOINTS.user(id), {
    headers: JSON_HEADERS,
    tags: { op: 'get_user' },
  });
}

export function listUsers() {
  return http.get(ENDPOINTS.users, {
    headers: JSON_HEADERS,
    tags: { op: 'list_users' },
  });
}

export function updateUser(id, user) {
  return http.put(ENDPOINTS.user(id), JSON.stringify(user), {
    headers: JSON_HEADERS,
    tags: { op: 'update_user' },
  });
}

export function deleteUser(id) {
  return http.del(ENDPOINTS.user(id), null, {
    headers: JSON_HEADERS,
    tags: { op: 'delete_user' },
  });
}

/** Reads the id out of a create response. Returns null if the body is unusable. */
export function idOf(response) {
  try {
    return response.json('id');
  } catch (e) {
    return null;
  }
}
