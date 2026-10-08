// Test data. Emails must be unique per virtual user per iteration so that
// concurrent writers never collide.

const FIRST_NAMES = ['Alice', 'Bob', 'Carol', 'Dave', 'Erin', 'Frank', 'Grace', 'Heidi'];
const DOMAINS = ['example.com', 'test.io', 'mail.dev'];

function pick(list) {
  return list[Math.floor(Math.random() * list.length)];
}

/**
 * Builds a user. `__VU` and `__ITER` are k6 built-ins (virtual user id and
 * iteration number), which gives a collision-free identifier without a
 * round-trip to the server.
 */
export function randomUser() {
  const local = `u${__VU}-${__ITER}-${Date.now()}`;
  return {
    name: `${pick(FIRST_NAMES)} ${__VU}-${__ITER}`,
    email: `${local}@${pick(DOMAINS)}`,
  };
}

/** Payload for an update: same shape, different values. */
export function changedUser() {
  const local = `upd${__VU}-${__ITER}-${Date.now()}`;
  return {
    name: `Updated ${__VU}-${__ITER}`,
    email: `${local}@example.com`,
  };
}
