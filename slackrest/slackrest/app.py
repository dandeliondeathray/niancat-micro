import threading
import slack
import os
from tornado.ioloop import IOLoop, PeriodicCallback
from tornado import gen
from tornado.httpclient import AsyncHTTPClient, HTTPRequest
import json
from slackrest.command import Method, CommandParser, Visibility
import sys
from collections import namedtuple

class InvalidResponseType(Exception):
    def __init__(self):
        Exception.__init__(self)

IncomingMessage = namedtuple("IncomingMessage", ["message", "channel_id", "user_id", "user_name"])

class SlackException(RuntimeError):
    def __init__(self, *args, **kwargs):
        RuntimeError.__init__(*args, **kwargs)

def read_token():
    try:
        token = os.environ["SLACK_API_TOKEN"].strip()
    except KeyError:
        print("You must specify a Slack API token in the 'SLACK_API_TOKEN' environment variable")
        return
    return token

def handle_callback_exception(callback):
    (ex_type, value, traceback) = sys.exc_info()
    print("EXCEPTION {}: {}", ex_type, value)
    print("Traceback: {}", traceback)
    IOLoop.current().stop()

def enqueue_response(message, channel_id):
    print("Replying to Slack with message {} and channel id {}".format(message, channel_id))
    #IOLoop.current().add_callback(self.sc.rtm_send_message, channel_id, message)

class RouteContext:
    def __init__(self, web_client, incoming_message, notification_channel_id):
        self.web_client = web_client
        self.incoming_message = incoming_message
        self.notification_channel_id = notification_channel_id

    def response_channel(self, response):
        response_type = response['response_type']
        if response_type == 'reply':
            response_channel = self.incoming_message.channel_id
        elif response_type == 'notification':
            response_channel = self.notification_channel_id
        else:
            raise InvalidResponseType()
        return response_channel

notification_channel_id = None
users = {}
command_parser = None
self_name = "" # FIXME

@slack.RTMClient.run_on(event="message")
def handle_message(**payload):
    print(f"Payload: {payload!s}")
    message = payload["data"]
    if 'subtype' not in message:
        web_client = payload["web_client"]
        channel_id = message['channel']
        user_id = message['user']
        try:
            user_name = users[user_id]
        except KeyError:
            user_name = '<unknown user name>'
        incoming_message = IncomingMessage(message, channel_id, user_id, user_name)
        print(f"Message: {incoming_message!s}")
        route_context = RouteContext(web_client, incoming_message, notification_channel_id)
        visibility = Visibility.parse(channel_id)
        # return self.handle_command(incoming_message, route_context, visibility)
        print(f"Visibility: {visibility!s}  Route context: {route_context!s}  Incoming message: {incoming_message!s}")
        request = command_parser.parse(incoming_message.message["text"],
                                       incoming_message.channel_id,
                                       incoming_message.user_id,
                                       incoming_message.user_name,
                                       self_name,
                                       visibility)
        if request:
            print(f"Request {request!s}")
        else:
            print("Ignoring message")
    else:
        print(f"Subtype message: {message!s}")

def run_forever(base_url, commands, not_channel_id):
    global notification_channel_id
    global users
    global command_parser
    token = read_token()
    if token is None:
        return
    web_client = slack.WebClient(token=token)
    users_list = web_client.users_list()
    #print(f"Users list: {users_list!s}")
    is_users_list_ok = users_list['ok']
    if not is_users_list_ok:
        print(f"Users list not OK: {users_list!s}")
        return
    users = {
        member['id']: member['name']
        for member in users_list['members']
    }
    command_parser = CommandParser(commands)
    client = slack.RTMClient(token=token)
    notification_channel_id = not_channel_id
    if client is None:
        return
    client.start()

# class RouteContext:
#     """
#     A RouteContext is a class which encapsulates all the context needed to route a response to the right channel/user.
#     """
#     def __init__(self, outbound_message_queue, incoming_message, notification_channel_id):
#         self.outbound_message_queue = outbound_message_queue
#         self.incoming_message = incoming_message
#         self.notification_channel_id = notification_channel_id

#     def route(self, response):
#         """
#         Route a response according to its `response_type` value.
#         `response_type` must be one of `reply`, `notification`, or `direct`. If it is `direct`, then the key
#         `channel_id` must also be present.

#         :param response: a dict with keys `response_type`, `message`, and optionally `channel_id`.
#         """
#         response_type = response['response_type']
#         if response_type == 'reply':
#             response_channel = self.incoming_message.channel_id
#         elif response_type == 'notification':
#             response_channel = self.notification_channel_id
#         else:
#             raise InvalidResponseType()

#         self.outbound_message_queue.enqueue(response['message'], response_channel)

# class RequestAndRouteContext:
#     def __init__(self, request, route_context):
#         self.request = request
#         self.route_context = route_context

# def handle_command(incoming_message, route_context, visibility):
#     print("Handling command for incoming message {}".format(incoming_message.message))
#     print("Bot user name is '{}' and sender user name is '{}'".format(self.self_name, incoming_message.user_name))
#     message_text = incoming_message.message['text']
#     request = self._command_parser.parse(message_text,
#                                             incoming_message.channel_id,
#                                             incoming_message.user_id,
#                                             incoming_message.user_name,
#                                             self.self_name,
#                                             visibility)
#     if request:
#         print("Request will be for URL {}".format(request.url))
#         return RequestAndRouteContext(request, route_context)
#     else:
#         print("Ignoring message")
#         return None

# class SlackrestApp(object):
#     def __init__(self, base_url, commands, notification_channel_id):
#         self.base_url = base_url
#         self._async_thread = None
#         self.sc = None
#         self.read_msg_callback = None
#         self.handler = MessageHandler(CommandParser(commands), self, notification_channel_id)

#     def run_async(self):
#         print("Starting SlackrestApp asynchronously")
#         self._async_thread = threading.Thread(target=self.run_forever)
#         self._async_thread.start()

#     @gen.coroutine
#     def make_request(self, request, route_context):
#         def response_callback(http_response):
#             if http_response.error:
#                 print("HTTP Response Error: {}".format(http_response.error))
#             else:
#                 response = json.loads(http_response.buffer.getvalue().decode("utf-8"))
#                 print("Response: {}".format(response))
#                 for r in response:
#                     route_context.route(r)

#         final_url = self.base_url + request.url
#         client = AsyncHTTPClient()
#         http_request = HTTPRequest(final_url, method=Method.serialize(request.method), body=request.body)
#         client.fetch(http_request, response_callback)

#     def enqueue(self, message, channel_id):
#         print("Replying to Slack with message {} and channel id {}".format(message, channel_id))
#         IOLoop.current().add_callback(self.sc.rtm_send_message, channel_id, message)

#     def connect_to_slack(self):
#         print("Connecting to Slack...")
#         self.sc = create_slack_client()
#         if not self.sc:
#             raise SlackException("Failed to create Slack client!")
#         self.sc.server.rtm_connect()
#         self.handler.set_self_name(self.sc.server.username)
#         print("Connected to Slack. Bot user name is '{}'".format(self.sc.server.username))

#     def read_slack_messages(self):
#         msgs = self.sc.rtm_read()
#         requests_and_route_contexts = self.handler.handle_messages(msgs, self.sc.server.users)
#         for rarc in requests_and_route_contexts:
#             self.make_request(rarc.request, rarc.route_context)



#     def run_forever(self):
#         print("Starting SlackrestApp...")
#         self.connect_to_slack()
#         self.read_msg_callback = PeriodicCallback(self.read_slack_messages, 500)
#         self.read_msg_callback.start()
#         IOLoop.current().handle_callback_exception = handle_callback_exception
#         IOLoop.current().start()
