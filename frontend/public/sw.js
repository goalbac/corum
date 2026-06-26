/* Service Worker — Web Push 알림 수신 */

self.addEventListener('push', (event) => {
  if (!event.data) return;

  let data = {};
  try {
    data = event.data.json();
  } catch {
    data = { title: event.data.text(), content: '', linkUrl: '/' };
  }

  const title   = data.title   || 'Corum 알림';
  const content = data.content || '';
  const linkUrl = data.linkUrl || '/';

  event.waitUntil(
    self.registration.showNotification(title, {
      body: content,
      icon: '/favicon.ico',
      badge: '/favicon.ico',
      data: { linkUrl },
      requireInteraction: false,
    })
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const linkUrl = (event.notification.data && event.notification.data.linkUrl) || '/';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if ('focus' in client) {
          client.focus();
          client.navigate(linkUrl);
          return;
        }
      }
      if (clients.openWindow) {
        return clients.openWindow(linkUrl);
      }
    })
  );
});
