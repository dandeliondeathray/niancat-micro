from queue import Queue, Empty
import tornado.websocket as ws
from tornado.ioloop import IOLoop
from tornado import gen
import json

class NoSuchEvent(RuntimeError):
    def __init__(self, *args, **kwargs):
        RuntimeError(*args, **kwargs)


class EventHandler(object):
    def __init__(self, ws_url, loop):
        self.queue = Queue()
        self.unread_messages = []
        self.ws_url = ws_url
        self.ws_connection = None
        self.loop = loop
        self.connect_websocket()

    def connect_websocket(self):
        print("Connecting EventHandler WebSocket to {}".format(self.ws_url))
        ws.websocket_connect(self.ws_url, io_loop=self.loop, callback=self.on_connected, on_message_callback=self.on_message_callback)

    def on_connected(self, ws_conn):
        print("EventHandler connected to {}".format(self.ws_url))
        self.ws_connection = ws_conn.result()

    def on_message_callback(self, msg):
        self.queue.put_nowait(json.loads(msg))

    def find_unread_message(self, type):
        for i in range(0, len(self.unread_messages)):
            msg = self.unread_messages[i]
            if msg['event'] == type:
                self.unread_messages = self.unread_messages[:i] + self.unread_messages[i+1:]
                return msg
        return None

    def send_message(self, msg):
        self.loop.add_callback(self.ws_connection.write_message, json.dumps(msg))

    def await_connected(self):
        self.await_event('hello')

    def await_event(self, event_type, timeout_ms=5000):
        unread_message = self.find_unread_message(event_type)
        if unread_message:
            return unread_message

        wait_ms = 500

        if timeout_ms < 0:
            assert False, "No event of type {} received!".format(event_type)

        slept = 0
        while slept < timeout_ms:
            try:
                msg = self.queue.get(block=True, timeout=wait_ms / 1000)
                if msg['event'] == event_type:
                    return msg
                else:
                    self.unread_messages.append(msg)
            except Empty:
                pass
            finally:
                slept += wait_ms
        assert False, "No event of type {} received!".format(event_type)
