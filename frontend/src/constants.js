// constants.js - Status definitions and HeadHunter-style design tokens

export const STATUS_LIST = [
  'WISHLIST',
  'APPLIED',
  'INTERVIEW',
  'OFFER',
  'REJECTED'
];

export const STATUS_CONFIG = {
  WISHLIST: {
    label: 'В планах',
    dotColor: 'bg-gray-400',
    badgeBg: 'bg-gray-100 text-gray-700 border-gray-200',
    columnBorder: 'border-t-2 border-t-gray-400',
    headerBg: 'bg-gray-50',
    emptyText: 'Добавьте вакансии, которые хотите рассмотреть',
  },
  APPLIED: {
    label: 'Откликнулся',
    dotColor: 'bg-blue-600',
    badgeBg: 'bg-blue-50 text-blue-700 border-blue-200',
    columnBorder: 'border-t-2 border-t-blue-600',
    headerBg: 'bg-blue-50/40',
    emptyText: 'Пока нет отправленных откликов',
  },
  INTERVIEW: {
    label: 'Собеседование',
    dotColor: 'bg-amber-500',
    badgeBg: 'bg-amber-50 text-amber-800 border-amber-200',
    columnBorder: 'border-t-2 border-t-amber-500',
    headerBg: 'bg-amber-50/40',
    emptyText: 'Нет назначенных интервью',
  },
  OFFER: {
    label: 'Оффер',
    dotColor: 'bg-emerald-600',
    badgeBg: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    columnBorder: 'border-t-2 border-t-emerald-600',
    headerBg: 'bg-emerald-50/40',
    emptyText: 'Здесь появятся ваши офферы',
  },
  REJECTED: {
    label: 'Отказ',
    dotColor: 'bg-rose-500',
    badgeBg: 'bg-rose-50 text-rose-700 border-rose-200',
    columnBorder: 'border-t-2 border-t-rose-500',
    headerBg: 'bg-rose-50/40',
    emptyText: 'Нет отказов',
  },
};
