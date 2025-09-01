import http from 'k6/http';
import {check, sleep} from "k6";

const API_BASE_URL = 'http://localhost:8080/api';

const TOTAL_PAGES = 1000;
const USERNAME = __ENV.K6_USERNAME || 'owner';
const PASSWORD = __ENV.K6_PASSWORD || 'owner123';
const USERAGENT = __ENV.K6_USER_AGENT || 'Mozilla/5.0 (Windows NT 10';

export const options = {
    stages: [
        {duration: '30s', target: 50},
        {duration: '2m', target: 50},
        {duration: '10s', target: 0},
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500'],
        'http_req_failed': ['rate<0.01'],
        'checks': ['rate>0.99']
    }
}


export function setup() {
    console.log('Running setup to authenticate user.');

    const loginPayload = JSON.stringify({
        username: USERNAME,
        password: PASSWORD,
        userAgent: USERAGENT,
    });

    const params = {
        headers: { 'Content-Type': 'application/json'}
    }

    const loginResponse = http.post(`${API_BASE_URL}/auth/login`, loginPayload, params);

    check(loginResponse, {
        'Authentication successful': (res) => res.status === 200
    })
;
    const token = loginResponse.json('data.accessToken')
    console.log('Authentication successful. Token received.');

    return { bearerToken: token };
}

export default function (data) {
    const params = {
        headers: { 'Authorization': `Bearer ${data.bearerToken}`}
    }

    // with 10.000 product data
    const randomPage = Math.floor(Math.random() * TOTAL_PAGES);

    // scenario 1. user's want to see the product list;
    const getListRes = http.get(`${API_BASE_URL}/products?page=${randomPage}&size=10`, params);

    const isListSuccess = check(getListRes, {
        'GET /products (list): status is 200': (response) => response.status === 200,
        'GET /products (list): response has "data" array': (response) => {
            try {
                return Array.isArray(response.json('data'));
            } catch (e) {
                return false;
            }
        }
    });

    // if the request is failed, we want to stop.
    if(!isListSuccess) {
        sleep(1);
        return;
    }

    const products = getListRes.json('data');

    // Scenario 2. User's want to see the specific data of a product.
    if (products.length > 0) {
        // Choose a random product;
        const randomProduct = products[Math.floor(Math.random() * products.length)];
        const productId = randomProduct.id;

        const getDetailResponse = http.get(`${API_BASE_URL}/products/${productId}`, params);

        // check the response
        check(getDetailResponse, {
            'GET /products/{productId} (detail}: status is 200': res => res.status === 200,
            'GET /products/{productId} (detail}: response has correct id': res => {
                try {
                    return res.json('data.id') === productId;
                } catch (e) {
                    return false;
                }
            }
        });
    }

    // scenario user's sees the data of product detail for a certain time.
    sleep(1);
}