import { options } from './scenarios/playlist_create.js';
import { setupLogin } from './setup/login.js';
import playlistScenario from './scenarios/playlist_create.js';

export { options };

export function setup() {
    return setupLogin();
}

export default function (data) {
    playlistScenario(data);
}