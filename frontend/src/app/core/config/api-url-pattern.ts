/**
 * Creates a regular expression matching an API base URL and its paths.
 *
 * @param baseUrl configured API base URL
 * @returns URL pattern suitable for the bearer-token interceptor
 */
export function createApiUrlPattern(baseUrl: string): RegExp {
  const normalizedUrl = baseUrl.replace(/\/+$/, '');

  const escapedUrl = normalizedUrl.replace(
    /[.*+?^${}()|[\]\\]/g,
    '\\$&',
  );

  return new RegExp(`^${escapedUrl}(?:/|$)`);
}
