import { CURRENCY_SYMBOL } from '../config';

const moneyFormat = new Intl.NumberFormat('en-IN', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatMoney(amount) {
  return `${CURRENCY_SYMBOL}${moneyFormat.format(amount)}`;
}

// Parses the parts by hand: new Date('2025-07-01') is UTC midnight and can show the previous day.
export function formatDate(iso) {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

export function formatDateTime(isoInstant) {
  return new Date(isoInstant).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
}

export function pluralize(count, word) {
  return `${count} ${word}${count === 1 ? '' : 's'}`;
}