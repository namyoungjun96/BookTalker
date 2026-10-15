// 백엔드 ReviewVisibilityEnum과 값 일치 필요
export const VISIBILITY_OPTIONS = [
  { value: 'PUBLIC', label: '전체공개', className: 'public' },
  { value: 'GROUP', label: '모임공개', className: 'group' },
  { value: 'PRIVATE', label: '비공개', className: 'private' },
];

export const DEFAULT_VISIBILITY = 'PRIVATE';

export const getVisibilityOption = (value) =>
  VISIBILITY_OPTIONS.find((option) => option.value === value)
  ?? VISIBILITY_OPTIONS.find((option) => option.value === DEFAULT_VISIBILITY);
