import { setupLogin } from './setup/login.js';
import { options } from './scenarios/ws-chat.js';
import realtimechat from './scenarios/ws-chat.js';

export { options };

export function setup() {
    return setupLogin();
}

export default function (data) {
    realtimechat(data);
}