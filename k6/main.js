import { setupLogin } from './setup/login.js';
import { options } from './scenarios/ws-chatLatency.js';
import realtimechat from './scenarios/ws-chatLatency.js';

export { options };

export function setup() {
    return {
        tokens: setupLogin(),
        scenarioStartTimeMs: Date.now(),
    };
}

export default function (data) {
    realtimechat(data);
}
