import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';

const WS_URL =
  import.meta.env.VITE_WS_URL ??
  (import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1')
    .replace(/^http/, 'ws')
    .replace(/\/api\/v1\/?$/, '/api/ws');

type Handler = (body: unknown) => void;

interface Listener {
  destination: string;
  handler: Handler;
  subscription?: StompSubscription;
}

/**
 * One STOMP connection for the whole app. Features call `subscribe` and get an unsubscribe
 * function back; the client connects lazily on the first subscriber, re-subscribes everything
 * after a reconnect, and disconnects when the last subscriber leaves.
 */
const listeners = new Set<Listener>();

const client = new Client({
  brokerURL: WS_URL,
  reconnectDelay: 3000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  // Read the token on every (re)connect — it may have been refreshed since the last one
  beforeConnect: () => {
    const token = localStorage.getItem('accessToken');
    client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
  },
  onConnect: () => listeners.forEach(attach),
  onStompError: (frame) => console.error('[WS] broker error', frame.headers.message),
});

function attach(listener: Listener) {
  listener.subscription = client.subscribe(listener.destination, (message: IMessage) => {
    try {
      listener.handler(JSON.parse(message.body));
    } catch (e) {
      console.error('[WS] bad message on', listener.destination, e);
    }
  });
}

export function subscribe(destination: string, handler: Handler): () => void {
  const listener: Listener = { destination, handler };
  listeners.add(listener);

  if (client.connected) attach(listener);
  else if (!client.active) client.activate();

  return () => {
    listeners.delete(listener);
    if (client.connected) listener.subscription?.unsubscribe();
    if (listeners.size === 0) void client.deactivate();
  };
}
