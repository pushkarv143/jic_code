/**
 * Validation patterns shared by the forms that edit existing records.
 *
 * These exist because a stricter rule than the data actually stored makes a
 * record uneditable: the form loads a value it considers invalid, validation
 * blocks submit on a field the user never touched, and the save button appears
 * to do nothing.
 */

/**
 * Phone numbers as they are actually stored — `+91-9810011122` throughout the
 * seed data, as well as plain `9810011122`.
 *
 * A digits-only `^[0-9]{10}$` rule rejects every seeded record. It also
 * contradicts the backend, whose own rule (StudentSelfUpdateRequest,
 * TeacherSelfUpdateRequest) is this same permissive shape — so the stricter
 * client rule rejected values the API would have accepted.
 *
 * Public-facing forms that only ever take fresh input (registration, admission
 * and contact enquiries) keep the stricter 10-digit rule on purpose: there is no
 * stored value to contradict, and a tighter rule is better at the point of entry.
 */
export const PHONE_PATTERN = /^[0-9+\-\s()]{6,20}$/;
export const PHONE_MESSAGE = 'Enter a valid phone number (digits, and optionally + - spaces)';

/** Indian PIN code. Seed data uses six digits, so this one matches reality already. */
export const PINCODE_PATTERN = /^[0-9]{6}$/;
export const PINCODE_MESSAGE = 'Enter a valid 6-digit pincode';
