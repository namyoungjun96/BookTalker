import axios from 'axios';
import { useToast } from 'vue-toastification';

// 환경 변수에서 API Base URL 가져오기
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

// axios 인스턴스 생성
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // 세션 쿠키를 위해
  headers: {
    'Content-Type': 'application/json',
  },
});

// Toast 인스턴스 (전역 사용)
let toast = null;

// Toast 초기화 함수 (main.js에서 앱이 마운트된 후 호출 가능)
export const initToast = () => {
  if (!toast) {
    toast = useToast();
  }
  return toast;
};

// 요청 인터셉터 (필요시 토큰 추가 등)
apiClient.interceptors.request.use(
  (config) => {
    // Toast 인스턴스가 없으면 초기화 시도
    if (!toast) {
      try {
        toast = useToast();
      } catch (e) {
        // 앱이 아직 마운트되지 않았을 수 있음
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터 (에러 처리)
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // Toast 인스턴스 확인
    if (!toast) {
      try {
        toast = useToast();
      } catch (e) {
        // Toast를 사용할 수 없으면 콘솔 로그
        console.error('Toast not available:', error);
      }
    }

    const status = error.response?.status;
    const config = error.config || {};
    
    // 커스텀 플래그 체크
    // 1. skipErrorHandler: true -> 모든 에러 Toast 무시
    // 2. skipErrorCodes: [404, 204] -> 특정 상태 코드만 Toast 무시
    const skipAll = config.skipErrorHandler === true;
    const skipCodes = config.skipErrorCodes || [];
    const shouldSkip = skipAll || skipCodes.includes(status);
    
    // Toast를 띄우지 않아야 하는 경우 바로 에러 반환
    if (shouldSkip) {
      return Promise.reject(error);
    }
    
    // 401 에러: 인증 실패
    if (status === 401) {
      if (toast) {
        toast.error('로그인이 필요합니다. 로그인 페이지로 이동합니다.');
      }
      // 1초 후 로그인 페이지로 리다이렉트
      setTimeout(() => {
        window.location.href = '/login';
      }, 1000);
    }
    // 403 에러: 권한 없음
    else if (status === 403) {
      if (toast) {
        toast.error('접근 권한이 없습니다.');
      }
    }
    // 404 에러: 리소스 없음
    else if (status === 404) {
      if (toast) {
        toast.warning('요청한 리소스를 찾을 수 없습니다.');
      }
    }
    // 500번대 에러: 서버 오류
    else if (status >= 500) {
      if (toast) {
        toast.error('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
      }
    }
    // 네트워크 오류
    else if (!error.response) {
      if (toast) {
        toast.error('네트워크 연결을 확인해주세요.');
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
export { API_BASE_URL };
