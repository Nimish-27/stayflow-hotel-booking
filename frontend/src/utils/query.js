// Builds "?a=1&b=2" and skips empty values. Returns '' when nothing is left.
export function buildSearch(params) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value) {
      query.set(key, value);
    }
  });
  const text = query.toString();
  return text ? `?${text}` : '';
}