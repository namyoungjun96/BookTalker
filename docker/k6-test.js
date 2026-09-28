import http from 'k6/http';
import { sleep } from 'k6';

const BASE_URL = 'http://host.docker.internal:8010';
const BOOKS = ['이방인', '구의 증명', '위대한 개츠비'];

function enterSite (naverId) {
    if (__ITER === 0) {
        http.post(`${BASE_URL}/test/login`, {naverId: naverId});
    }
    http.get(`${BASE_URL}/rank`);
}

export function reviewJourney() {
    const naverId = `test-user-${__VU}`;
    enterSite(naverId);

    const query = encodeURIComponent(pickRandomBook());

    sleep(3);

    const searchResponse = http.get(`${BASE_URL}/book/search?query=${query}&start=1&maxResults=10&cover=Small`);

    if (searchResponse.status !== 200) {
        return;
    }

    let body

    try {
        body = JSON.parse(searchResponse.body);
    } catch (e) {
        console.log('PARSE_FAIL', __VU, __ITER, searchResponse.status, searchResponse.url);
        return;
    }

    if (!body.items || body.items.length === 0) {
        return ;
    }

    sleep(10);

    const aladinBook = body.items[0];

    const payload = JSON.stringify({
        isbn13: aladinBook.isbn13,
        writer: naverId,
        headline: 'k6 headline',
        content: 'k6 content',
        rating: Math.floor(Math.random() * 5) + 1,
        isPublic: true
    })

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    http.post(`${BASE_URL}/review`, payload, params);
}

function pickRandomBook() {
    const idx = Math.floor(Math.random() * BOOKS.length);
    return BOOKS[idx];
}

export function mypageJourney() {
    const naverId = `test-user-${__VU}`;
    enterSite(naverId);

    sleep(3);

    http.get(`${BASE_URL}/review/list`);
}

export const options = {
    noCookiesReset: true,
    scenarios: {
        reviewJourney: {
            executor: 'ramping-vus',
            exec: 'reviewJourney',
            stages: [
                { duration: '15s', target: 5 },
                { duration: '15s', target: 5 },
                { duration: '15s', target: 10 },
                { duration: '15s', target: 10 },
                { duration: '15s', target: 15 },
                { duration: '15s', target: 15 },
                { duration: '15s', target: 20 },
                { duration: '15s', target: 20 },
                { duration: '15s', target: 30 },
                { duration: '15s', target: 30 },
                { duration: '15s', target: 20 },
                { duration: '15s', target: 20 },
                { duration: '15s', target: 10 },
                { duration: '15s', target: 10 },
                { duration: '15s', target: 0 },
                { duration: '15s', target: 0 },
            ],
        },
        mypageJourney: {
            executor: 'constant-vus',
            exec: 'mypageJourney',
            vus: 5,
            duration: '3m45s',
        },
    },
};